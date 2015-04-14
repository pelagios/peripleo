define(['search/events'], function(Events) {
  
  var infoBoxTemplate =
    '<div>' +
    
    '</div>';
  
  var SelectionInfoBox = function(container, eventBroker) {
    var element = jQuery(
          '<div id="selection-info">' +
          '  <h3></h3>' +
          '  <p class="names"></p>' +
          '  <p class="description"></p>' +
          '  <ul class="uris"></ul>' +
          '</div>'),
          
        currentPlace = false,
        
        label = element.find('h3'),
        
        names = element.find('.names'),
        
        description = element.find('.description'),
        
        uris = element.find('.uris'),
        
        fillTemplate = function(place) {          
          label.html(place.label);
          names.html(place.names.join(', '));
          description.html(place.description);
          
          uris.append(jQuery('<li>' + formatGazetteerURI(place.gazetteer_uri) + '</li>'));
          jQuery.each(place.matches, function(idx, uri) {
            uris.append(jQuery('<li>' + formatGazetteerURI(uri) + '</li>'));
          });
        },
        
        clearTemplate = function() {
          label.empty();
          names.empty();
          description.empty();
          uris.empty();
        },
        
        showPlace = function(place) {
          if (!place.gazetteer_uri)
            return; 
            
          if (currentPlace) {
            // Currently open
            if (!place) {
              // Close
              element.slideToggle(100, function() {
                currentPlace = false;
                clearTemplate();
              });
            } else {
              if (currentPlace.gazetteer_uri !== place.gazetteer_uri) {
                // New place - reset
                currentPlace = place;
                clearTemplate();
                fillTemplate(place);
              }
            }
          } else {
            // Currently closed - open
            if (place) {
              currentPlace = place;
              element.slideToggle(100);
              fillTemplate(place);
            }
          }  
        };
       
    element.hide();
    container.append(element);
    
    eventBroker.addHandler(Events.UI_SELECT_PLACE, showPlace);
  };
  
  return SelectionInfoBox;
  
});
