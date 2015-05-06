/** The result list **/
define(['search/events', 'common/formatting'], function(Events, Formatting) {

  var SLIDE_DURATION = 200,
  
      KEEP_OPEN_PERIOD = 500;

  var ResultList = function(parent, eventBroker) {
    var element = jQuery(
          '<div id="search-results">' +
          '  <ul></ul>' +
          '</div>'),
          
        margin, 
        
        list = element.find('ul'),
        
        /** TODO revisit - do we need this? **/
        pendingQuery = false,
        
        /** Helper flag - forces the list to ignore updates and hide()-calls **/
        ignoreUpdates = false,
        
        /** Helper flag - indicates that the current list represents a sub-search result **/
        subsearch = false,
          
        /** The currently buffered search results **/
        currentResults = [],
        
        /** Checks current height and limits to max screen height **/
        constrainHeight = function() {
          var top = element.position().top,
              height = element.outerHeight(true),
              availableHeight = parent.height() - top;
                        
          element.css({ height: 'auto' }); 
          if (height > availableHeight)
            element.css('height', availableHeight - margin);
        },
                
        /** Toggles visibility of the result list **/
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

        /** Hides the result list **/
        hide = function() {
          if (!ignoreUpdates)
            element.slideUp(SLIDE_DURATION, function() { element.css({ height: 'auto' }); });
        },
        
        /** 
         * Shows the result list. If no opt_results are provided, the list
         * will show the globally buffered currentResults
         * 
         * @param opt_results a result list (leave undefined to show currentResults)
         */
        show = function(opt_results) {
          var results = (opt_results) ? opt_results : currentResults;

          if (!element.is(':visible')) { // Currently hidden - show
            if (results.length > 0) {
              rebuildList(results);
              element.slideDown({ duration: SLIDE_DURATION, complete: constrainHeight });
            }
          } else { // Just replace the list
            element.css({ height: 'auto' });  
            element.slideUp(SLIDE_DURATION, function() {
              rebuildList(results);
              element.css({ height: 'auto' });
              element.slideDown(SLIDE_DURATION, constrainHeight);
            });
          }
        },        
        
        /** Rebuilds the list element **/
        rebuildList = function(results) {  
          var rows = jQuery.map(results, function(result) {
            var li, icon, html;
            
            switch (result.object_type.toLowerCase()) {
              case 'place': 
                icon = '<span class="icon" title="Place">&#xf041;</span>';
                break;
              default:
                icon = '<span class="icon" title="Item">&#xf219;</span>';
            }
            
            html = '<li><h3>' + icon + ' ' + result.title + '</h3>';
              
            if (result.names)
              html += '<p class="names">' +
                result.names.slice(0, 8).join(', ') + '</p>';

            if (result.description) 
              html += '<p class="description">' + result.description + '</p>';
              
              
            if (result.object_type === 'Place') {
              html += '<ul class="uris">';
              html += Formatting.formatGazetteerURI(result.identifier);

              if (result.matches)
                jQuery.each(result.matches, function(idx, uri) {
                  html += Formatting.formatGazetteerURI(uri);
                });
              
              html += '</ul>';
            }
            
            if (result.snippet)
              html += result.snippet;
              
            if (result.dataset_path)
              html += '<p class="source">Source:' +
                      ' <span data-id="' + result.dataset_path[0].id + '">' + result.dataset_path[0].title + '</span>' +
                      '</p>';
              
            html += '</li>';
              
            li = jQuery(html);
            li.mouseenter(function() { eventBroker.fireEvent(Events.MOUSE_OVER_RESULT, result); });
            li.mouseleave(function() { eventBroker.fireEvent(Events.MOUSE_OVER_RESULT); });
            
            li.click(function() {
              eventBroker.fireEvent(Events.SELECT_RESULT, result);
            });
                          
            return li;
          });
          
          list.empty();
          list.append(rows);
        };      
        
    
    
    parent.append(element);
    element.css('max-height', parent.height() - element.position().top);
    element.hide();
    
    margin = parseInt(element.css('margin-top'));

    eventBroker.addHandler(Events.CONTROLS_ANIMATION, function() { 
      element.css({ height: 'auto' });  
      constrainHeight();
    });
    
    eventBroker.addHandler(Events.CONTROLS_ANIMATION_END, function() { 
      element.css({ height: 'auto' });  
      constrainHeight();
    });
    
    // Listen for search results
    eventBroker.addHandler(Events.API_SEARCH_RESPONSE, function(response) {
      subsearch = false;
      currentResults = response.items;
      
      // The map will change after search response - we want to ignore change
      // and update requests in this case
      ignoreUpdates = true; 
      
      // If there was a user-supplied query or place filter we open automatically
      if (response.params.query || response.params.place) { 
        rebuildList(response.items);
        element.slideDown(SLIDE_DURATION, constrainHeight);
      }
      
      setTimeout(function() { ignoreUpdates = false; }, KEEP_OPEN_PERIOD);
    });
    
    // Listen for sub-search results
    eventBroker.addHandler(Events.API_SUB_SEARCH_RESPONE, function(response) {
      subsearch = true;
      show(response.items);
    });
    
    // View updates (and initial view load)
    eventBroker.addHandler(Events.API_VIEW_UPDATE, function(response) {
      currentResults = response.items;
    });
    
    eventBroker.addHandler(Events.API_INITIAL_RESPONSE, function(response) {
      currentResults = response.items;
    });
    
    // We want to know about user-issued queries, because after
    // a "user-triggered" (rather than "map-triggered") search
    // returns, we want the list to open automatically
    eventBroker.addHandler(Events.SEARCH_CHANGED, function(change) {
      pendingQuery = change.query;
    });
    
    // Like Google Maps, we close the result list when the user
    // resumes map browsing, or selects a result
    eventBroker.addHandler(Events.VIEW_CHANGED, hide);

    // Manual open/close events
    eventBroker.addHandler(Events.TOGGLE_ALL_RESULTS, toggle); 
    eventBroker.addHandler(Events.SHOW_ALL_RESULTS, show); 
    eventBroker.addHandler(Events.HIDE_ALL_RESULTS, hide); 
  };
  
  return ResultList;
  
});
