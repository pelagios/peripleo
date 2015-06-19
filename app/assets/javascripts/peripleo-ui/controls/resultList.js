/** The result list **/
define(['common/formatting', 'peripleo-ui/events/events'], function(Formatting, Events) {

  var SLIDE_DURATION = 180, OPEN_DELAY = 380;

  var ResultList = function(container, eventBroker) {
    
    var element = jQuery('<div id="search-results"><ul></ul></div>'),

        /** DOM element shorthands **/       
        list = element.find('ul'),
          
        /** Most recent search results **/
        currentSearchResults = [],
        
        /** Most recent subsearch results **/
        currentSubsearchResults = [],
        
        /** Checks current height and limits to max screen height *
        constrainHeight = function() {
          var height, availableHeight;
          
          if (element.is(':visible')) {
            element.css({ height: 'auto' });  
            
            top = element.position().top;   
            height = element.outerHeight(true);
            availableHeight = parent.height() - top;
                
            if (height > availableHeight)
              element.css('height', availableHeight - margin);
              
          }
        },*/
                
        /** Toggles visibility of the result list **
        toggle = function() {
          if (element.is(':visible')) { // List visible
            if (subsearch) { // The list shows sub-search results - show currentResults instead
              subsearch = false;
              show();
            } else { // If the list shows the currentResults, hide it
              hide();
            }
          } else { // List hidden - show it
            show();
          }
        },
        */
        
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
              html = '<li><h3>' + icon + result.title + '</h3>';

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
          
          return jQuery(html + '</li>');
        }
                
        rebuildList = function(results) {
          var rows = jQuery.map(results, function(result) {
            var li = renderResult(result);
            li.mouseenter(function() { eventBroker.fireEvent(Events.MOUSE_OVER_RESULT, result); });
            li.mouseleave(function() { eventBroker.fireEvent(Events.MOUSE_OVER_RESULT); });
            li.click(function() {
              hide();                
              eventBroker.fireEvent(Events.SELECT_RESULT, [ result ]);
            });
            return li;
          });
          
          list.empty();
          list.append(rows);          
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
        show = function(results) {
          rebuildList(results);
          if (!element.is(':visible'))
            element.velocity('slideDown', { duration: SLIDE_DURATION, delay: OPEN_DELAY });
        };
          
          
       /*
          if (!element.is(':visible')) { // Currently hidden - show
            if (results.length > 0) {
              rebuildList(results);
              element.velocity('slideDown', { duration: SLIDE_DURATION, complete: constrainHeight });
            }
          } else { // Just replace the list
            element.css({ height: 'auto' });  
            element.velocity('slideUp', 
              { duration:SLIDE_DURATION, 
                complete: function() {
                  rebuildList(results);
                  element.css({ height: 'auto' });
                  element.velocity('slideDown', { duration: SLIDE_DURATION, complete: constrainHeight });
                }
              });
          }
        };       */
        
        /** Rebuilds the list element *
        rebuildList = function(results) {  

        };    
        */  

    element.hide();    
    container.append(element);
    
    // margin = parseInt(element.css('margin-top'));
    // top = element.position().top;    
    // resetMaxHeight();
 
    /* Reset max-height after window resized
    jQuery(window).resize(function() {
      setTimeout(resetMaxHeight, 100);
    });
    */
  
    // eventBroker.addHandler(Events.CONTROLS_ANIMATION, constrainHeight);
    // eventBroker.addHandler(Events.CONTROLS_ANIMATION_END, constrainHeight);
    
    /* Listen for search results
    eventBroker.addHandler(Events.API_SEARCH_RESPONSE, function(response) {
      subsearch = false;
      currentResults = response.items;
      
      // The map will change after search response - we want to ignore change
      // and update requests in this case
      ignoreUpdates = true; 
      
      // If there was a user-supplied query or place filter we open automatically
      if (response.params.query || response.params.place) { 
        rebuildList(response.items);
        element.velocity('slideDown', { duration: SLIDE_DURATION, complete: constrainHeight });
      }
      
      setTimeout(function() { ignoreUpdates = false; }, KEEP_OPEN_PERIOD);
    });
    */
    
    /* Listen for sub-search results
    eventBroker.addHandler(Events.API_SUB_SEARCH_RESPONSE, function(response) {
      subsearch = true;
      show(response.items);
    });
    */
    
    // Initial response
    eventBroker.addHandler(Events.API_INITIAL_RESPONSE, function(response) {
      currentSearchResults = response.items;
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
      
      // TODO update control contents
      // - If there's a query phrase -> open
      // - If it's open, update
      
    });
    
    // Sub-search
    eventBroker.addHandler(Events.API_SUB_SEARCH_RESPONSE, function(response) {
      currentSubsearchResults = response.items;
      show(currentSubsearchResults); // Show immediately      
    });

    // Manual open/close events
    // eventBroker.addHandler(Events.TOGGLE_ALL_RESULTS, toggle); 
    // eventBroker.addHandler(Events.SHOW_ALL_RESULTS, show); 
    // eventBroker.addHandler(Events.HIDE_ALL_RESULTS, hide); 
  };
  
  return ResultList;
  
});
