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
    
    var featureGroup = L.featureGroup().addTo(map),
    
        selectionPin = false,
        
        pendingQuery = false,
        
        allowMouseOverHighlights = true,
    
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
          if (place.geometry.type == 'Polygon' && 
              place.geometry.coordinates[0].length === 5) {
              
            place.geometry.type = 'Point';
            place.geometry.coordinates = [
              (place.geo_bounds.max_lon + place.geo_bounds.min_lon) / 2,
              (place.geo_bounds.max_lat + place.geo_bounds.min_lat) / 2 ];
          }
        },
        
        /** Computes a 'location hash code' - objects with the same location/geometry share the same hash **/
        locationHashCode = function(object) {

        },
        
        /** Shorthand: resets location and zoom of the map to fit all current objects **/
        fitToObjects = function() {
          if (!jQuery.isEmptyObject(objects)) {
            map.fitBounds(featureGroup.getBounds(), {
              animate: true,
              paddingTopLeft: [380, 20],
              paddingBottomRight: [20, 20],
              maxZoom: 9
            });
          }
        },
        
        /** Highlights (and returns) the object with the specified id **/
        highlight = function(id) {
          var tuple, latlon;
          
          if (selectionPin) {
            map.removeLayer(selectionPin);
            selectionPin = false;
          }
          
          if (id) {
            tuple = objects[id];    
            
            if (tuple) {
              latlon = tuple.marker.getBounds().getCenter();
              selectionPin = L.marker(latlon).addTo(map);
              return tuple;
            }
          }          
        },
        
        /** Shorthand: highlights the object and triggers the select event **/
        select = function(id) {
          var tuple;
          
          if (id) {
            tuple = highlight(id);

            if (tuple)
              eventBroker.fireEvent(Events.UI_SELECT_PLACE, tuple.obj);
          } else {
            if (selectionPin)
              map.removeLayer(selectionPin);
            eventBroker.fireEvent(Events.UI_SELECT_PLACE);
          }
        },
        
        /** Clears all ojbects from the map **/
        clear = function() {
          highlight();
          featureGroup.clearLayers();          
          objects = {};
        },
        
        addPlace = function(p) {                    
          var marker, uri = p.identifier, type,
              cLon = (p.geo_bounds) ? (p.geo_bounds.max_lon + p.geo_bounds.min_lon) / 2 : false,
              cLat = (p.geo_bounds) ? (p.geo_bounds.max_lat + p.geo_bounds.min_lat) / 2 : false;
          
          if (p.geo_bounds) { // Ignore places without geometry
          
            // Get rid of Barrington grid squares    
            collapseRectangles(p);
          
            if (!exists(uri)) {
              type = p.geometry.type;
              
              if (type === 'Point') {
                marker = L.circleMarker([cLat, cLon], Styles.SMALL);
              } else if (type === 'Polygon' || type === 'LineString' || type === 'MultiPolygon') {
                marker = L.geoJson(p.geometry, Styles.POLYGON);
              } else {
                console.log('Unsupported convex hull type: ' + p.geometry.type , p);
              }
            
              if (marker) {
                marker.on('click', function(e) { select(uri); return false; });
                objects[uri] = { obj: p, marker: marker };
                marker.addTo(featureGroup);
              }
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
          
          fitToObjects();
        },
      
        /**
         * Selects the object or place closest to the given latlng.
         * 
         * This is primarily a means to support touch devices. Markers are
         * otherwise too small that you could properly tap them.
         */
        selectNearest = function(latlng) {
          var xy = map.latLngToContainerPoint(latlng),
              nearest = { distSq: 9007199254740992 },
              nearestXY, distPx;
              
          jQuery.each(objects, function(id, t) {
            var markerLatLng = t.marker.getBounds().getCenter(),
                distSq = 
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
              select(nearest.obj.identifier);
            else
              select();
          } else {
            select();
          }
        };
        
    // We want to know about user-issued queries, because as long
    // as a user query is 'active', we don't want to add/remove
    // stuff from the map
    eventBroker.addHandler(Events.UI_SEARCH, function(query) {
      if (pendingQuery !== query) // New query - clear the map
        clear();
        
      pendingQuery = query;
    });
        
    // See above - we only update the map if there was a new search
    eventBroker.addHandler(Events.API_SEARCH_SUCCESS, function(results) {
      if (pendingQuery)
        addObjects(results);
      pendingQuery = false;
    });
    
    eventBroker.addHandler(Events.UI_MOUSE_OVER_RESULT, function(result) {
      var id = (result) ? result.identifier : false;
      
      if (allowMouseOverHighlights) // See below!
        highlight(id);
    });
    
    // This event can be triggered from the objectLayer or the resultList
    // Highlight the marker when the trigger comes from the result list
    eventBroker.addHandler(Events.UI_SELECT_PLACE, function(result) {
      if (result) {
        var tuple = highlight(result.identifier),
            markerLatLng = tuple.marker.getBounds().getCenter();
          
        if (!map.getBounds().contains(markerLatLng))
          map.panTo(markerLatLng);
      
        // Note: there can be accidential mouseovers as the result list closes
        // Make sure we have a 'grace period' for that, in which mouseovers
        // are ignored
        allowMouseOverHighlights = false;
        setTimeout(function() { allowMouseOverHighlights = true; }, 500);
      } else {
        highlight();
      }
    });
    
    map.on('click', function(e) { selectNearest(e.latlng); });
  };
  
  return ObjectLayer;
  
});
