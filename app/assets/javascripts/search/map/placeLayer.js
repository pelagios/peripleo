define(function() {

  var PlaceLayer = function(map) {
    var layerGroup = L.layerGroup().addTo(map),
    
        markers = {},
    
        setPlaces = function(places) {
          var uris = jQuery.map(places, function(p) { return p.gazetteer_uri });
          
          // Add places that are not already on the map
          jQuery.each(places, function(idx, place) {
            if (!markers[place.gazetteer_uri]) {
              markers[place.gazetteer_uri] = 
                L.marker([ place.centroid_lat, place.centroid_lng ])
                 .addTo(layerGroup)
                 .bindPopup(place.label);
            }
          });
          
          // Now go through all markers and remove those that are not in the update
          jQuery.each(markers, function(uri, marker) {
            if (uris.indexOf(uri) < 0) {
              var icon = marker._icon,
                  shadow = marker._shadow;
              
              jQuery(icon).fadeOut(2000);
              jQuery(shadow).fadeOut(2000, function() {
                layerGroup.removeLayer(marker);
              });

              delete markers[uri];
            }
          });
        },
        
        clear = function() {
          // layerGroup.clearLayers();
        };

    this.setPlaces = setPlaces;
        
  };
  
  return PlaceLayer;

});
