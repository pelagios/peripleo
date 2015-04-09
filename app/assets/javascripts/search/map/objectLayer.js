define(['search/events'], function(Events) {
  
  var TOUCH_DISTANCE_THRESHOLD = 18,
  
      Styles = {
    
        SMALL: {
          color: '#a64a40',
          opacity: 1,
          fillColor: '#e75444',
          fillOpacity: 1,
          weight:1.5,
          radius:5
        },
      
        LARGE: {
          color: '#a64a40',
          opacity: 1,
          fillColor: '#e75444',
          fillOpacity: 1,
          weight:1.5,
          radius:9
        }
        
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
        select = function(id) {
          var tuple, latlon;
          
          if (selectionPin)
            map.removeLayer(selectionPin);
          
          if (id) {
            tuple = objects[id];
            latlon = tuple.marker.getLatLng();
                
            selectionPin = L.marker(latlon).addTo(map);
            eventBroker.fireEvent(Events.SELECT_PLACE, tuple.obj);
          }
        },
        
        clear = function() {
          layerGroup.clearLayers();          
          objects = {};
          selectionPin = false;
        },
                
        /** Adds places delivered in JSON place format **/
        addPlaces = function(places) {
          jQuery.each(places, function(idx, p) {
            var marker, uri = p.gazetteer_uri;
            
            if (!exists(uri)) {
              marker = L.circleMarker([p.centroid_lat, p.centroid_lng], Styles.SMALL);
              marker.on('click', function(e) { select(uri); });

              objects[uri] = { obj: p, marker: marker };
              marker.addTo(layerGroup);
            }
          });          
        },
            
        /** Adds objects (items or places) delivered in standard search result object JSON format **/
        addObjects = function(items) {
          /*
          jQuery.each(items, function(idx, item) {
            var lat = (item.geo_bounds) ? (item.geo_bounds.min_lat + item.geo_bounds.max_lat) / 2: false,
                lon = (item.geo_bounds) ? (item.geo_bounds.min_lon + item.geo_bounds.max_lon) / 2: false,
                marker;
                
            // TODO show full polygon?
            if (lat) {
              marker = L.circleMarker([lat, lon], Styles.LARGE);
              marker.on('click', function(e) { select(item.identifier); });

              objects[item.identifier] = { obj: item, marker: marker };
              marker.addTo(layerGroup);
            }
          });
          */
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
        
    // When the user issues a new query, we flush all objects
    eventBroker.addHandler(Events.QUERY, clear);
    
    this.addPlaces = addPlaces;
    this.addObjects = addObjects;
    this.selectNearest = selectNearest;
  };
  
  return ObjectLayer;
  
});
