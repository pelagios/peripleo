define(['search/events'], function(Events) {
  
  var FilterSettingsPopup = function(eventBroker) {
    var element = jQuery(
          '<div class="clicktrap">' +
          '  <div id="filter-editor">' +
          '   <span class="close">CLOSE</span>' +
          '  </div>' +
          '</div>'
        ),
        
        btnClose = element.find('.close'),
        
        editFilterSettings = function(dimension) {
          element.show();  
        };
      
    element.hide();
    jQuery(document.body).append(element);
    
    btnClose.click(function() { element.hide(); });
    
    eventBroker.addHandler(Events.EDIT_FILTER_SETTINGS, editFilterSettings);
    
  };
  
  return FilterSettingsPopup;
    
});
