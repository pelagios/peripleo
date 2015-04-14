/** The result list **/
define(['search/events', 'common/formatting'], function(Events, Formatting) {

  var ResultList = function(container, eventBroker) {
    var element = jQuery(
          '<div id="search-results">' +
          '  <ul></ul>' +
          '</div>'),

        list = element.find('ul'),
          
        currentResults = [],
        
        /**
         * Updates the results. If the UI element is closed,
         * the function just buffers the results for later.
         * If the UI element is open, the function rebuilds the
         * list.
         */
        update = function(results) {
          currentResults = results.items;
          
          if (element.is(':visible')) {
            list.empty();
            rebuildList();
          }
        },
        
        toggle = function() {
          if (element.is(':visible'))
            hide();
          else
            show();
        },
        
        show = function() {
          rebuildList();
          element.show();
        },
        
        hide = function() {
          element.hide();
        },
        
        /**
         * Rebuilds the list element from the current results.
         */
        rebuildList = function() {          
          var rows = jQuery.map(currentResults, function(result) {
            var li, icon, html;
            console.log(result);
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
              html += '<p class="names"><span class="icon">' + 
              result.names.splice(0,8).join(', ') + '</p>';

            if (result.description) 
              html += '<p class="description">' + result.description + '</p>';
              
            if (result.matches) {
              html += '<ul class="uris">';
              
              // It has matches - it's a place, so we know the id is a gazetteer URI
              html += Formatting.formatGazetteerURI(result.identifier);
              jQuery.each(result.matches, function(idx, uri) {
                html += Formatting.formatGazetteerURI(uri);
              });
              html += '</ul>';
            }
            
            if (result.snippet)
              html += '<p class="snippet">' + result.snippet + '</p>';
              
            html += '</li>';
              
            li = jQuery(html);
            li.on('mouseenter', function() {
              eventBroker.fireEvent(Events.UI_MOUSE_OVER_RESULT, result);
            });
                          
            return li;
          });
          
          list.empty();
          list.append(rows);
        };      
      
    element.hide();
    container.append(element);
    
    eventBroker.addHandler(Events.API_SEARCH_SUCCESS, update);   
    eventBroker.addHandler(Events.UI_TOGGLE_ALL_RESULTS, toggle); 
    eventBroker.addHandler(Events.UI_SHOW_ALL_RESULTS, show); 
    eventBroker.addHandler(Events.UI_HIDE_ALL_RESULTS, hide); 
  };
  
  return ResultList;
  
});
