define(['search/events', 'common/formatting'], function(Events, Formatting) {
  
  var SelectionInfoBox = function(container, eventBroker) {
    
    var SLIDE_DURATION = 200;
    
    var element = jQuery(
          '<div id="selection-info">' +
          '  <h3></h3>' +
          '  <p class="names"></p>' +
          '  <p class="description"></p>' +
          '  <ul class="uris"></ul>' +
          '</div>'),
          
        currentObject = false,
        
        title = element.find('h3'),
        
        names = element.find('.names'),
        
        description = element.find('.description'),
        
        uris = element.find('.uris'),
        
        fillTemplate = function(obj) {          
          title.html(obj.title);
          
          if (obj.names)
            names.html(obj.names.slice(0, 8).join(', '));
          
          if (obj.description)
            description.html(obj.description);
          
          if (obj.object_type === 'Place') {
            uris.append(jQuery('<li>' + Formatting.formatGazetteerURI(obj.identifier) + '</li>'));
            if (obj.matches) {
              jQuery.each(obj.matches, function(idx, uri) {
                uris.append(jQuery('<li>' + Formatting.formatGazetteerURI(uri) + '</li>'));
              });
            }
          }
        },
        
        clearTemplate = function() {
          title.empty();
          names.empty();
          description.empty();
          uris.empty();
        },

        showObject = function(obj) {      
          if (currentObject) { // Box is currently open    
            if (!obj) { // Close it
              element.slideToggle(SLIDE_DURATION, function() {
                currentObject = false;
                clearTemplate();
              });
            } else {
              if (currentObject.identifier !== obj.identifier) { // New object - reset
                currentObject = obj;
                clearTemplate();
                fillTemplate(obj);
              }
            }
          } else { // Currently closed 
            if (obj) { // Open
              currentObject = obj;
              element.slideToggle(SLIDE_DURATION);
              fillTemplate(obj);
            }
          }  
        },
        
        hide = function() {
          currentObject = false;
          clearTemplate();
          element.slideUp(SLIDE_DURATION);
        };
       
    element.hide();
    container.append(element);
    
    eventBroker.addHandler(Events.UI_SELECT_PLACE, showObject);
    eventBroker.addHandler(Events.UI_SEARCH, hide);
  };
  
  return SelectionInfoBox;
  
});
