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
        
        icon = element.find('.icon'),
        
        autoComplete = new AutoComplete(form, input);
       
    updateIcon = function() {
      var chars = input.val().trim();
      
      if (chars.length === 0) {
        icon.html('&#xf002;');
        icon.removeClass('clear');
      } else {
        icon.html('&#xf00d;');
        icon.addClass('clear');
      }
    },
    
    clearSearch = function() {
      autoComplete.clear();
      form.submit();
      updateIcon();
    };
    
    // Set up events
    form.submit(function(e) {
      var chars = input.val().trim();

      if (chars.length === 0) {
        eventBroker.fireEvent(Events.QUERY_PHRASE_CHANGED, false);
        eventBroker.fireEvent(Events.SEARCH_CHANGED, { query : false });
      } else {
        eventBroker.fireEvent(Events.QUERY_PHRASE_CHANGED, chars);
        eventBroker.fireEvent(Events.SEARCH_CHANGED, { query : chars });
      }
    
      input.blur();
      return false; // preventDefault + stopPropagation
    });
    
    form.keypress(function (e) {
      updateIcon();
      if (e.which == 13) {
        form.submit();
        return false; // Otherwise we'll get two submit events
      }
    });
       
    form.on('click', '.clear', clearSearch);
        
    // Append to container
    container.append(element);
    
    // Fill with intial query, if any
    eventBroker.addHandler(Events.LOAD, function(initialSettings) {
      if (initialSettings.query) {
        input.val(initialSettings.query);
        updateIcon();
      }
    });
  };
  
  return SearchBox;
  
});
