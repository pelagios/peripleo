/** The result list **/
define(['search/events', 'common/formatting'], function(Events, Formatting) {

  var SLIDE_DURATION = 200;

  var ResultList = function(container, eventBroker) {
    var element = jQuery(
          '<div id="search-results">' +
          '  <ul></ul>' +
          '</div>'),

        list = element.find('ul'),
        
        pendingQuery = false,
        
        keepOpen = false,
          
        currentResults = [],
        
        /** Checks current height and limits to max screen height **/
        constrainHeight = function() {
          // TODO revise!
          var windowHeight = jQuery(window).outerHeight(),
              elTop = element.position().top,
              elHeight = element.outerHeight(),
              marginAndPadding = element.outerHeight(true) - element.height(),
              maxHeight = windowHeight - elTop - 2 * marginAndPadding;
          
          if (elHeight > maxHeight) 
            element.css({ height: maxHeight, maxHeight: maxHeight });          
        },
        
        /**
         * Updates the results. If the UI element is closed,
         * the function just buffers the results for later.
         * If the UI element is open, the function rebuilds the
         * list.
         */
        update = function(results) {
          currentResults = results.items;
          
          if (pendingQuery || element.is(':visible')) {
            list.empty();
            rebuildList();
          }
          
          if (pendingQuery) {
            if (currentResults.length > 0)
              element.slideDown(SLIDE_DURATION, constrainHeight);
              
            pendingQuery = false;
          }
        },
        
        toggle = function() {
          if (element.is(':visible'))
            hide();
          else
            show();
        },
        
        show = function() {
          if (currentResults.length > 0) {
            rebuildList();
            element.slideDown(SLIDE_DURATION, constrainHeight);
          }
        },
        
        hide = function() {
          if (!keepOpen) {
            element.slideUp(SLIDE_DURATION);
            element.css({ height: 'auto', maxHeight: 'none' });     
          }
        },
        
        /**
         * Rebuilds the list element from the current results.
         */
        rebuildList = function() {          
          var rows = jQuery.map(currentResults, function(result) {
            var li, icon, html;
            
            switch (result.object_type.toLowerCase()) {
              case 'place': 
                icon = '<span class="icon" title="Place">&#xf041;</span>';
                break;
              default:
                icon = '';
            }
            
            html = 
              '<li>' +
              '  <h3>' + icon + ' ' + result.title + '</h3>';
              
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
              html += '<p class="snippet">' + result.snippet + '</p>';
              
            html += '</li>';
              
            li = jQuery(html);
            li.mouseenter(function() {
              eventBroker.fireEvent(Events.UI_MOUSE_OVER_RESULT, result);
            });
            li.click(function() {
              hide();
              eventBroker.fireEvent(Events.UI_SELECT_PLACE, result);
            });
                          
            return li;
          });
          
          list.empty();
          list.append(rows);
        };      
      
    element.hide();
    container.append(element);
    
    element.mouseleave(function() {
      eventBroker.fireEvent(Events.UI_MOUSE_OVER_RESULT);
    });

    // Listen for search results
    eventBroker.addHandler(Events.API_SEARCH_SUCCESS, function(results) {
      // The map will change automatically after the search - in this case 
      // we don't want to close the panel, so allow for a short grace period
      keepOpen = true; 
      update(results);
      setTimeout(function() { keepOpen = false; }, 500);
    });   
    
    // We want to know about user-issued queries, because after
    // a "user-triggered" (rather than "map-triggered") search
    // returns, we want the list to open automatically
    eventBroker.addHandler(Events.UI_SEARCH, function(query) {
      hide();
      pendingQuery = query;
    });
    
    // Like Google Maps, we close the result list when the user
    // resumes map browsing
    eventBroker.addHandler(Events.UI_MAP_CHANGED, hide);

    // Manual open/close events
    eventBroker.addHandler(Events.UI_TOGGLE_ALL_RESULTS, toggle); 
    eventBroker.addHandler(Events.UI_SHOW_ALL_RESULTS, show); 
    eventBroker.addHandler(Events.UI_HIDE_ALL_RESULTS, hide); 
  };
  
  return ResultList;
  
});
