define(['peripleo-ui/events/events'], function(Events) {
  
  var Toolbar = function(container, eventBroker) {
    var btnHelp =
          jQuery('<div class="tool button icon" id="toolbar-help">&#xf128;</div>'),
    
        btnSettings =
          jQuery('<div class="tool button icon" id="toolbar-settings">&#xf013;</div>'),
        
        btnZoom = jQuery(
          '<div class="tool" id="toolbar-zoom">' +
          '  <div class="button icon" id="toolbar-zoom-in">&#xf067;</div>' +
          '  <div class="button icon" id="toolbar-zoom-out">&#xf068;</div>' +
          '</div>'),
          
        btnZoomIn = btnZoom.find('#toolbar-zoom-in'),
        btnZoomOut = btnZoom.find('#toolbar-zoom-out'); 
        
    btnSettings.click(function() { eventBroker.fireEvent(Events.EDIT_MAP_SETTINGS); });
    btnZoomIn.click(function() { eventBroker.fireEvent(Events.ZOOM_IN); });
    btnZoomOut.click(function() { eventBroker.fireEvent(Events.ZOOM_OUT); });
    
    container.append(btnHelp);
    container.append(btnSettings);
    container.append(btnZoom);
    
  };
  
  return Toolbar;
  
});
