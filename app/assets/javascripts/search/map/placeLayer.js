define(function() {
  
  var PlaceLayer = function(map) {
    var layerGroup = L.layerGroup().addTo(map),
    
        addPlace = function(place) {
          L.circleMarker([ place.centroid_lat, place.centroid_lng ])
           .addTo(layerGroup)
           .bindPopup(place.label);
          console.log('add');
        },
        
        clear = function() {
          layerGroup.clearLayers();
        };

    this.addPlace = addPlace;
    this.clear = clear;
        
  };
  
  return PlaceLayer;

});
