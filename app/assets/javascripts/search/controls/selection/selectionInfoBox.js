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
        
        formatGazetteerURI = function(uri) {
          var prefix, id;
          
          if (uri.startsWith('http://pleiades.stoa.org/places/')) {
            prefix = 'pleiades';
            id = uri.substring(32);
          } else if (uri.startsWith('http://dare.ht.lu.se/places/')) {
            prefix = 'dare';
            id = uri.substring(28);
          } else if (uri.startsWith('http://gazetteer.dainst.org/place/')) {
            prefix = 'idai';
            id = uri.substring(34);
          } else if (uri.startsWith('http://vici.org/vici/')) {
            prefix = 'vici';
            id = uri.substring(21);
          } else if (uri.startsWith('http://chgis.hmdc.harvard.edu/placename/')) {
            prefix = 'chgis';
            id = uri.substring(44);
          } else {
            // Bit of a hack...
            prefix = 'http';
            id = uri.substring(5);
          }
          
          return '<a class="gazetteer-uri ' + prefix + '" target="_blank" title="' + uri + '" href="' + uri + '">' + prefix + ':' + id + '</a>'; 
        },
        
        fillTemplate = function(place) {
          label.html(place.label);
          
          names.html(place.names.join(', '));
          description.html(place.description);
          
          uris.html('<li>' + formatGazetteerURI(place.gazetteer_uri) + '</li>');
          jQuery.each(place.matches, function(idx, uri) {
            uris.append('<li>' + formatGazetteerURI(uri) + '</li>');
          });
        },
        
        clearTemplate = function() {
          label.empty();
          names.empty();
          description.empty();
        },
        
        showPlace = function(place) {
          if (currentPlace) {
            // Currently open
            if (!place) {
              // Close
              element.slideUp(100, function() {
                currentPlace = false;
                clearTemplate();
              });
            } else {
              if (currentPlace.gazetteer_uri !== place.gazetteer_uri) {
                // New place - re-open
                element.slideUp(100, function() {
                  currentPlace = place;
                  fillTemplate(place);
                  element.slideDown(100);
                });
              }
            }
          } else {
            // Currently closed
            if (place) {
              currentPlace = place;
              fillTemplate(place);
              element.slideDown(100);
            }
          }  
        };
       
    element.hide();
    container.append(element);
    
    eventBroker.addHandler(Events.SELECT_PLACE, showPlace);
  };
  
  return SelectionInfoBox;
  
});
