define(['search/events'], function(Events) {
  
  var TOUCH_DISTANCE_THRESHOLD = 18,
  
      Styles = {
    
        SMALL: {
          color: '#a64a40',
          opacity: 1,
          fillColor: '#e75444',
          fillOpacity: 1,
          weight:1.5,
          radius:5,
          dropShadow:true
        },
      
        LARGE: (function(){
          // Just clone small style and change radius
          var style = jQuery.extend({}, this.SMALL);
          style.radius = 9;
          return style;
        })()
        
      };
      
  var ObjectLayer = function(map, eventBroker) {
    
    var layerGroup = L.layerGroup().addTo(map),
    
        selectionPin = false,
    
        /** (idOrURI -> { obj, marker }) map of places and items **/
        objects = {},
        
        /** Returns true if a marker for the specified URI exists on the map **/
        exists = function(idOrURI) {
          return objects.hasOwnProperty(idOrURI);
        },
        
        /** Selects the object with the specified URI or identifier **/
        select = function(objOrPlace) {
          if (selectionPin)
            map.removeLayer(selectionPin);
          
          if (objOrPlace) {
            selectionPin = L.marker([objOrPlace.centroid_lat, objOrPlace.centroid_lng ]).addTo(map);
            eventBroker.fireEvent(Events.SELECT_PLACE, objOrPlace);
          }
        };
                
        /** Adds places delivered in JSON place format **/
        addPlaces = function(places) {
          jQuery.each(places, function(idx, p) {
            var marker, uri = p.gazetteer_uri;
            
            if (!exists(uri)) {
              marker = L.circleMarker([p.centroid_lat, p.centroid_lng], Styles.SMALL);
              marker.on('click', function(e) {                
                select(p);
              });
              
              objects[uri] = { obj: p, marker: marker };
              marker.addTo(layerGroup);
            }
          });          
        },
            
        /** Adds objects (items or places) delivered in standard search result object JSON format **/
        addObjects = function(items) {

        },
      
        /** Selects the object or place closest to the given latlng **/
        selectNearest = function(latlng) {
          var xy = map.latLngToContainerPoint(latlng),
              nearest = { distSq: 9007199254740992 },
              nearestXY, distPx;
              
          jQuery.each(objects, function(id, t) {
            var markerLatLng = t.marker.getLatLng()
            var distSq = 
              Math.pow(latlng.lat - markerLatLng.lat, 2) + 
              Math.pow(latlng.lng - markerLatLng.lng, 2);  
                   
            if (distSq < nearest.distSq)
              nearest = { obj: t.obj, latlng: markerLatLng, distSq: distSq };
          });
          
          if (nearest.obj) {
            nearestXY = map.latLngToContainerPoint(nearest.latlng);
            distPx = 
              Math.sqrt(
                Math.pow((xy.x - nearestXY.x), 2) + 
                Math.pow((xy.y - nearestXY.y), 2));
          
            if (distPx < TOUCH_DISTANCE_THRESHOLD)
              select(nearest.obj);
            else
              select();
          }
        };
    
    this.addPlaces = addPlaces;
    this.addObjects = addObjects;
    this.selectNearest = selectNearest;
  };
  
  return ObjectLayer;
  
});
