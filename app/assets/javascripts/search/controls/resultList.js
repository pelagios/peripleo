/** The result list **/
define(['search/events'], function(Events) {

  var ResultList = function(container, eventBroker) {
    var element = jQuery(
          '<div id="search-results">' +
          '  <ul></ul>' +
          '</div>'),
          
        list = element.find('ul'),
        
        constrainHeight = function() {
          /*
          var windowHeight = jQuery(window).outerHeight(),
              elTop = element.position().top,
              elHeight = element.outerHeight(),
              marginAndPadding = element.outerHeight(true) - element.height(),
              maxHeight = windowHeight - elTop - marginAndPadding;
              
          if (elHeight > maxHeight) 
            element.css({ height: maxHeight, maxHeight: maxHeight });
          */
        },
          
        showResults = function(results) {
          var rows = jQuery.map(results, function(result) {
            var li, html = 
              '<li>' +
              '  <h3>' + result.title + '</h3>';

            if (result.description) 
              html += '<p class="description">' + result.description + '</p>';
            
            if (result.snippet)
              html += result.snippet;
              
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
