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
          alert(uri);
          
          if (uri.startsWith('http://pleiades.stoa.org/places/')) {
            alert('pleiades');
            prefix = 'pleiades';
            id = uri.substr(32);
            alert('id');
          } else if (uri.startsWith('http://dare.ht.lu.se/places/')) {
            prefix = 'dare';
            id = uri.substr(28);
          } else if (uri.startsWith('http://gazetteer.dainst.org/place/')) {
            prefix = 'idai';
            id = uri.substr(34);
          } else if (uri.startsWith('http://vici.org/vici/')) {
            prefix = 'vici';
            id = uri.substr(21);
          } else if (uri.startsWith('http://chgis.hmdc.harvard.edu/placename/')) {
            prefix = 'chgis';
            id = uri.substr(44);
          } else {
            // Bit of a hack...
            prefix = 'http';
            id = uri.substr(5);
          }
          
          return prefix + ':' + id;
          // return '<a class="gazetteer-uri ' + prefix + '" target="_blank" title="' + uri + '" href="' + uri + '">' + prefix + ':' + id + '</a>'; 
        },
        
        fillTemplate = function(place) {
          var uriLIs = [];
          
          label.html(place.label);
          names.html(place.names.join(', '));
          description.html(place.description);
          
          uriLIs.push(jQuery('<li>' + formatGazetteerURI(place.gazetteer_uri) + '</li>'));
          jQuery.each(place.matches, function(idx, uri) {
            uriLIs.push(jQuery('<li>' + formatGazetteerURI(uri) + '</li>'));
          });
          
          alert(uriLIs);
          alert(uris);
          uris.html(uriLIs);
          alert(uris);
        },
        
        clearTemplate = function() {
          label.empty();
          names.empty();
          description.empty();
          uris.empty();
        },
        
        showPlace = function(place) {
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
    
    eventBroker.addHandler(Events.SELECT_PLACE, showPlace);
  };
  
  return SelectionInfoBox;
  
});
