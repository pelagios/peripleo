/** The result list **/
define(['search/events', 'common/formatting'], function(Events, Formatting) {

  var ResultList = function(container, eventBroker) {
    var element = jQuery(
          '<div id="search-results">' +
          '  <ul></ul>' +
          '</div>'),
          
        list = element.find('ul'),
        
        constrainHeight = function() {
          var windowHeight = jQuery(window).outerHeight(),
              elTop = element.position().top,
              elHeight = element.outerHeight(),
              marginAndPadding = element.outerHeight(true) - element.height(),
              maxHeight = windowHeight - elTop - marginAndPadding;
              
          if (elHeight > maxHeight) 
            element.css({ height: maxHeight, maxHeight: maxHeight });
        },
          
        showResults = function(results) {
          var rows = jQuery.map(results, function(result) {
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
              eventBroker.fireEvent(Events.HOVER_RESULT, result);
            });
                          
            return li;
          });

          list.empty();
          list.append(rows);
          
          constrainHeight();
        };        
          
    element.hide();
    container.append(element);
    eventBroker.addHandler(Events.UPDATED_SEARCH_RESULTS, showResults);   
    eventBroker.addHandler(Events.LIST_ALL_RESULTS, function() { element.show() }); 
  };
  
  return ResultList;
  
});
