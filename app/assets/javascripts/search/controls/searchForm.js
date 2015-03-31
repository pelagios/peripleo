/** The search input form **/
define(['search/events'], function(Events) {
  
  var SearchForm = function(container, eventBroker) {
    var element = jQuery(
          '<div id="query-container">' +
          '  <form>' +
          '    <span class="icon">&#xf002;</span>' +
          '    <input type="text" id="query" name="query" placeholder="Search..." autocomplete="off">' +
          '  </form>' +
          '</div>'),
          
        form = element.find('form'),
        
        input = form.find('input');
        
    // Set up events
    form.submit(function(e) {
      eventBroker.fireEvent(Events.SEARCH, input.val());
      input.blur();
      return false; // preventDefault + stopPropagation
    });
    
    form.keypress(function (e) {
      if (e.which == 13)
        form.submit();
    });
        
    // Append to container
    container.append(element);
  };
  
  return SearchForm;
  
});
