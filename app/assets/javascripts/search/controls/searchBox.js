/** The search input form **/
define(['search/controls/autoComplete', 'search/events'], function(AutoComplete, Events) {
  
  var SearchBox = function(container, eventBroker) {
    var element = jQuery(
          '<div id="searchbox">' +
          '  <form>' +
          '    <input type="text" id="query" name="query" autocomplete="off">' +
          '    <span class="icon">&#xf002;</span>' +
          '  </form>' +
          '</div>'),
          
        form = element.find('form'),
        
        input = form.find('input'),
        
        autoComplete = new AutoComplete(form, input);
        
    // Set up events
    form.submit(function(e) {
      var query = input.val().trim();
      
      if (query.length === 0) {
        // Make sure handlers get query === undefined for empty strings
        eventBroker.fireEvent(Events.UI_SEARCH);
      } else {
        eventBroker.fireEvent(Events.UI_SEARCH, query);
      }
    
      input.blur();
      return false; // preventDefault + stopPropagation
    });
    
    form.keypress(function (e) {
      if (e.which == 13) {
        form.submit();
        return false; // Otherwise we'll get two submit events
      }
    });
        
    // Append to container
    container.append(element);
  };
  
  return SearchBox;
  
});
