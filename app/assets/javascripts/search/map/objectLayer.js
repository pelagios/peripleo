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
        },
        
        POLYGON: {
          color: '#a64a40',
          opacity: 1,
          fillColor: '#e75444',
          fillOpacity: 0.65,
          weight:1.5
        }
        
      };
      
  var ObjectLayer = function(map, eventBroker) {
    
    var layerGroup = L.layerGroup().addTo(map),
    
        selectionPin = false,
    
        /** (idOrURI -> { obj, marker }) map of places and items **/
        objects = {},
        
        /** Returns true if a marker for the specified URI exists on the map **/
        exists = function(identifier) {
          return objects.hasOwnProperty(identifier);
        },
        
        /** 
         * Hack: 'normalizes' a GeoJSON geometry in place, by collapsing
         * rectangular polygons to centroid points. This is because rectangles
         * are usually from Barrington grid squares, and we don't want those in 
         * the UI.
         */
        collapseRectangles = function(place) {          
          if (place.convex_hull.type == 'Polygon' && 
              place.convex_hull.coordinates[0].length === 5) {
              
            place.convex_hull.type = 'Point';
            place.convex_hull.coordinates = [
              (place.geo_bounds.max_lon + place.geo_bounds.min_lon) / 2,
              (place.geo_bounds.max_lat + place.geo_bounds.min_lat) / 2 ];
          }
        },
        
        /** Computes a 'location hash code' - objects with the same location/geometry share the same hash **/
        locationHashCode = function(object) {

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
        
        addPlace = function(p) {          
          var marker, uri = p.identifier,
              cLon = (p.geo_bounds) ? (p.geo_bounds.max_lon + p.geo_bounds.min_lon) / 2 : false,
              cLat = (p.geo_bounds) ? (p.geo_bounds.max_lat + p.geo_bounds.min_lat) / 2 : false;
          
          // Get rid of Barrington grid squares    
          collapseRectangles(p);
          
          if (!exists(uri)) {
            if (p.convex_hull.type === 'Point') {
              marker = L.circleMarker([cLat, cLon], Styles.SMALL);
            } else if (p.convex_hull.type === 'Polygon' || p.convex_hull.type === 'LineString') {
              marker = L.geoJson(p.convex_hull, Styles.POLYGON);
            } else {
              console.log('Unsupported convex hull type: ' + p.convex_hull.type , p);
            }
            
            if (marker) {
              marker.on('click', function(e) { select(uri); });
              objects[uri] = { obj: p, marker: marker };
              marker.addTo(layerGroup);
            }
          }           
        },
        
        addItem = function(item) {
          console.log('addItem not implemented yet');
        },
        
        addDataset = function(dataset) {
          console.log('addDataset not implemented yet');          
        },
        
        addObjects = function(response) {
          jQuery.each(response.items, function(idx, obj) {
            var t = obj.object_type;
            
            if (t === 'Place') {
              addPlace(obj);
            } else if (t === 'Item') {
              addItem(obj);
            } else if (t === 'Dataset') {
              addDataset(obj);
            } else {
              console.log('Invalid search result!', obj);
            }
          });
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
        
    eventBroker.addHandler(Events.UI_SEARCH, clear);
    eventBroker.addHandler(Events.API_SEARCH_SUCCESS, addObjects);
    
    map.on('click', function(e) { selectNearest(e.latlng); });
  };
  
  return ObjectLayer;
  
});
