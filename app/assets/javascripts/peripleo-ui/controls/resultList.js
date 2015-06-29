/** The result list **/
define(['common/formatting', 'peripleo-ui/events/events'], function(Formatting, Events) {

  var SLIDE_DURATION = 180, OPEN_DELAY = 380,
  
      SearchState = { SEARCH : 1, SUB_SEARCH : 2 };

  var ResultList = function(container, eventBroker) {
    
    var element = jQuery(
          '<div id="search-results">' +
          '  <ul></ul>' +
          '  <div id="wait-for-next"></div>' +
          '</div>'),

        /** DOM element shorthands **/       
        list = element.find('ul'),        
        waitForNextIndicator = element.find('#wait-for-next'),
          
        /** Most recent search results **/
        currentSearchResults = [],
        currentSearchResultsTotal,
        
        /** Most recent subsearch results **/
        currentSubsearchResults = [],
        currentSubsearchResultsTotal,
        
        /** Current search state **/
        currentSearchState = SearchState.SEARCH,
                
        /**
         * Helper that generates the appropriate icon span for a result.
         * 
         * This will get more complex as we introduce more types in the future.
         */
        getIcon = function(result) {
          if (result.object_type === 'Place')
            return '<span class="icon" title="Place">&#xf041;</span>';
          else 
            return '<span class="icon" title="Item">&#xf219;</span>';
        },
        
        /** Creates the HTML for a single search result entry **/
        renderResult = function(result) {
          var icon = getIcon(result),
              html = '<li><h3>' + icon + result.title + '</h3>',
              element;

          if (result.temporal_bounds) {
            html += '<p class="temp-bounds">';
            if (result.temporal_bounds.start === result.temporal_bounds.end)
              html += Formatting.formatYear(result.temporal_bounds.start);
            else 
              html += Formatting.formatYear(result.temporal_bounds.start) + ' - ' + Formatting.formatYear(result.temporal_bounds.end);
            html += '</p>';
          }
              
          if (result.names)
            html += '<p class="names">' + result.names.slice(0, 8).join(', ') + '</p>';

          if (result.description) 
            html += '<p class="description">' + result.description + '</p>';
              
          if (result.object_type === 'Place') {
            html += '<ul class="uris">' + Formatting.formatGazetteerURI(result.identifier);

            if (result.matches)
              jQuery.each(result.matches, function(idx, uri) {
                  html += Formatting.formatGazetteerURI(uri);
                });
              
            html += '</ul>';
          }
          
          if (result.dataset_path)
            html += '<p class="source">Source:' +
                    ' <span data-id="' + result.dataset_path[0].id + '">' + result.dataset_path[0].title + '</span>' +
                    '</p>';
          
          // Add event handlers
          element = jQuery(html + '</li>');
          element.mouseenter(function() { eventBroker.fireEvent(Events.MOUSE_OVER_RESULT, result); });
          element.mouseleave(function() { eventBroker.fireEvent(Events.MOUSE_OVER_RESULT); });
          element.click(function() {
            hide();                
            eventBroker.fireEvent(Events.SELECT_RESULT, [ result ]);
          });
          
          return element;
        }
                
        renderList = function(results, append) {
          var rows = jQuery.map(results, function(result) {
            return renderResult(result);
          });
          
          if (!append)
            list.empty();
            
          list.append(rows);      
        },
        
        scrollTop = function() {
          element.scrollTop(0);   
        },
        
        /** If scrolled to bottom, we load the next result page if needed **/ 
       onScroll = function() {
          var scrollPos = element.scrollTop() + element.innerHeight(),
              scrollBottom = element[0].scrollHeight,
              loadedResults;
              
          // TODO visual wait indication
          
          if (scrollPos >= scrollBottom) {
            if (currentSearchState === SearchState.SEARCH) {
              loadedResults = currentSearchResults.length;
              if (currentSearchResultsTotal > loadedResults)
                eventBroker.fireEvent(Events.LOAD_NEXT_PAGE, loadedResults);
            } else {
              loadedResults = currentSubsearchResults.length;
              if (currentSubsearchResultsTotal > loadedResults)
                eventBroker.fireEvent(Events.LOAD_NEXT_PAGE, loadedResults);
            }
          }
        },
        
        /** Hides the result list **/
        hide = function() {
          if (element.is(':visible'))
            element.velocity('slideUp', { duration: SLIDE_DURATION });
        },
                
        /** 
         * Shows a list of results.
         * 
         * The function will open the panel automatically if it is not yet open. 
         */
        show = function(results, opt_delay) {
          renderList(results); 
          if (element.is(':visible'))
            scrollTop();
          else
            element.velocity('slideDown', { duration: SLIDE_DURATION, delay: opt_delay, complete: scrollTop });
        },
        
        /** API delivered the next page of search results **/
        onNextPage = function(response) {
          if (currentSearchState === SearchState.SEARCH)
            currentSearchResults = currentSearchResults.concat(response.items);
          else
            currentSubsearchResults = currentSubsearchResults.concat(response.items);
            
          renderList(response.items, true);
        };

    element.scroll(onScroll);
    element.hide();    
    container.append(element);

    // Initial response
    eventBroker.addHandler(Events.API_INITIAL_RESPONSE, function(response) {
      currentSearchResults = response.items;
      currentSearchResultsTotal = response.total;
    });
    
    // View updates - like GMaps, we close when user resumes map browsing
    eventBroker.addHandler(Events.VIEW_CHANGED, hide);
    
    eventBroker.addHandler(Events.API_VIEW_UPDATE, function(response) {
      currentSearchResults = response.items;
      
      // TODO how to update control contents? 
      // - Don't update?
      // - Update after wait? --> Probably best. But don't close/re-open the panel
      // - Update only in case there's no search query
      
    });
    
    // Search
    eventBroker.addHandler(Events.SEARCH_CHANGED, function(diff) {
      
      // TODO if panel open, clear it and show 'loading' spinner
      // TODO here we can also track if there's a search phrase or not
      
    });
    
    eventBroker.addHandler(Events.API_SEARCH_RESPONSE, function(response) {
      currentSearchResults = response.items;
      currentSearchResultsTotal = response.total;
      
      // TODO update control contents
      // - If there's a query phrase -> open
      // - If it's open, update
      
    });
    
    // Sub-search
    eventBroker.addHandler(Events.API_SUB_SEARCH_RESPONSE, function(response) {
      currentSubsearchResults = response.items;
      currentSearchResultsTotal = response.total;
      show(currentSubsearchResults, OPEN_DELAY); // Show immediately      
    });
    
    // Next page of search results available
    eventBroker.addHandler(Events.API_NEXT_PAGE, onNextPage);
    
    // (De)selection via map
    eventBroker.addHandler(Events.SELECT_MARKER, hide);

    // Manual open/close events
    eventBroker.addHandler(Events.SHOW_ALL_RESULTS, function() { 
      currentSearchState = SearchState.SEARCH;
      show(currentSearchResults); 
    }); 
    
    eventBroker.addHandler(Events.SHOW_SUBSEARCH_RESULTS, function() { 
      currentSearchState = SearchState.SUB_SEARCH;
      show(currentSubsearchResults); 
    });
    
    eventBroker.addHandler(Events.HIDE_RESULTS, hide); 
  };
  
  return ResultList;
  
});
