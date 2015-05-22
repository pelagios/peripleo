define(['search/events'], function(Events) {
  
  var TOUCH_DISTANCE_THRESHOLD = 18,
  
      BASE_STYLE = {
        color: '#a64a40',
        opacity: 1,
        fillColor: '#e75444',
        fillOpacity: 1,
        weight:1.5,
        radius:5
      },
  
      Styles = {
    
        SMALL: (function() { return jQuery.extend({}, BASE_STYLE); })(),
        
        SMALL_GREY: (function() { 
          var style = jQuery.extend({}, BASE_STYLE);
          style.color = '#959595';
          style.fillColor = '#aeaeae';
          return style;
        })(),
      
        LARGE: (function() { 
          var style = jQuery.extend({}, BASE_STYLE);
          style.radius = 9;
          return style;
        })(),
        
        POLYGON: (function() { 
          var style = jQuery.extend({}, BASE_STYLE);
          style.color = '#db473a';
          style.fillColor = '#db473a';
          style.fillOpacity = 0.12;
          style.weight = 0.75;
          return style;
        })()
                
      };
      
  var ObjectLayer = function(map, eventBroker) {
    
        /** One feature group to hold all overlays **/   
    var featureGroup = L.featureGroup().addTo(map),
        
        /** Map[id -> (object, marker)] to support 'findById'-type queries **/
        objectIndex = {},
        
        /** Map[geometryHash -> (marker, Array<object>)] to support 'findByGeometryHash'-type queries **/
        markerIndex = {},
        
        /** A tuple (marker, Array<object>) **/        
        currentSelection = false,
        
        /** The map pin highlighting the currently emphasised marker **/
        emphasisPin = false,
        
        /** 
         * Creates a string representation of a GeoJSON geometry to be used as a
         * key in the marker index. (The only requirements are that the representation
         * is unique for every possible geometry, and that identical geometries
         * will result in the same representation.)
         */
        createGeometryHash = function(geometry) {
          return JSON.stringify(geometry);
        },
        
        /** Tests if the object with the specified ID exists on the object layer **/
        objectExists = function(id) {
          return objectIndex.hasOwnProperty(id);
        },
                
        /** 
         * An unfortunate hack we need due to the ugliness introduced by Pleiades'
         * Barrington grid squares. We don't want the grid squares to mess up the UI,
         * so this function 'normalizes' a GeoJSON geometry (mutating it in place), by
         * collapsing rectangular polygons to centroid points.
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
        
        /** Updates the object layer with a new search response or view update **/
        update = function(objects, invalidateMarkers) {
          // Set all markers to 'out-of-filter'
          if (invalidateMarkers)
            featureGroup.setStyle(Styles.SMALL_GREY);
          
          jQuery.each(objects, function(idx, obj) {
            var id = obj.identifier,
                existingObjectTuple = objectIndex[id],

                geomHash = (obj.geo_bounds) ? createGeometryHash(obj.geometry) : false,
                existingMarkerTuple = (geomHash) ? markerIndex[geomHash] : false,
                  
                type, marker;
          
            if (geomHash) { // No need to bother if there is no geometry
              collapseRectangles(obj); // Get rid of Barrington grid squares
                
              if (existingObjectTuple) {
                jQuery.extend(existingObjectTuple._1, obj); // Object exists - just update the data
                existingObjectTuple._2.setStyle(Styles.SMALL);
                existingObjectTuple._2.bringToFront();
              } else {                  
                if (existingMarkerTuple) { // There's a marker at that location already - add the object
                  existingMarkerTuple._2.push(obj); 
                  marker = existingMarkerTuple._1;
                  marker.setStyle(Styles.SMALL);
                  marker.bringToFront();
                } else { // Create and add a new marker
                  type = obj.geometry.type;
                  if (type === 'Point')
                    marker = L.circleMarker([obj.geo_bounds.max_lat, obj.geo_bounds.max_lon], Styles.SMALL);
                  else
                    marker = L.geoJson(obj.geometry, Styles.POLYGON);
          
                  marker.on('click', function(e) { selectByGeomHash(geomHash); return false; });
                  markerIndex[geomHash] = { _1: marker, _2: [obj] };
                  marker.addTo(featureGroup); 
                }

                objectIndex[id] = { _1: obj, _2: marker };
              }
            }
          });
        },
        
        /** Helper method that resets map location and zoom to fit all current objects **/
        fitToObjects = function() {
          if (!jQuery.isEmptyObject(markerIndex)) {
            map.fitBounds(featureGroup.getBounds(), {
              animate: true,
              paddingTopLeft: [380, 20],
              paddingBottomRight: [20, 20],
              maxZoom: 9
            });
          }
        },
        
        /** Clears all ojbects from the map **/
        clear = function() {
          clearSelection();
          featureGroup.clearLayers();          
          objectIndex = {};
          markerIndex = {};
        },
        
        /** Function that emphasises the marker passed to it **/
        emphasise = function(marker) {  
          var latlon;
                  
          if (emphasisPin) {
            map.removeLayer(emphasisPin);
            emphasisPin = false;
          }
          
          if (marker) {
            latlon = marker.getBounds().getCenter();
            emphasisPin = L.marker(latlon).addTo(map);
          }          
        }
        
        /** Selects (and emphasises) the marker with the specified geometry hash **/
        selectByGeomHash = function(geomHash) {    
          currentSelection = markerIndex[geomHash]; // (marker, Array<object>)
          if (currentSelection) {
            emphasise(currentSelection._1); 
            // TODO fire event
          }
        },
        
        /** Selects (and emphasises) the marker linked to the object with the specified ID **/
        selectById = function(id) {
          /*
          var tuple;
          
          if (id) {
            tuple = highlight(id);

            if (tuple) {
              currentSelection = tuple.obj.identifier;
              eventBroker.fireEvent(Events.SELECT_MARKER, tuple.obj);
              return tuple;
            }
          } else {
            currentSelection = false;
            if (selectionPin)
              map.removeLayer(selectionPin);
            eventBroker.fireEvent(Events.SELECT_MARKER);
          }
           */
        },
        
        clearSelection = function() {
          emphasise();
          currentSelection = false;
        },
        
        /**
         * Selects the marker nearest the given latlng. This is primarily a
         * means to support touch devices, where touch events will usually miss 
         * the markers because they are too small for properly hitting them.
         */
        selectNearest = function(latlng, maxDistance) {
          var xy = map.latLngToContainerPoint(latlng),
              nearest = { distSq: 9007199254740992 }, // Distance to nearest initialied with Integer.MAX
              nearestXY, distPx;
              
          jQuery.each(markerIndex, function(geomHash, tuple) {
            var markerLatLng = tuple._1.getBounds().getCenter(),
                distSq = 
                  Math.pow(latlng.lat - markerLatLng.lat, 2) + 
                  Math.pow(latlng.lng - markerLatLng.lng, 2);  
                   
            if (distSq < nearest.distSq)
              nearest = { geomHash: geomHash, latlng: markerLatLng, distSq: distSq };
          });
          
          if (nearest.geomHash) {
            nearestXY = map.latLngToContainerPoint(nearest.latlng);
            distPx = 
              Math.sqrt(
                Math.pow((xy.x - nearestXY.x), 2) + 
                Math.pow((xy.y - nearestXY.y), 2));
          
            if (distPx < maxDistance)
              selectByGeomHash(nearest.geomHash);
            else
              clearSelection();
          } else {
            clearSelection();
          }
        };
     
    // TODO only for touch?
    map.on('click', function(e) { selectNearest(e.latlng, TOUCH_DISTANCE_THRESHOLD); });

    eventBroker.addHandler(Events.API_VIEW_UPDATE, function(results) {
      update(results.top_places);
    });
        
    eventBroker.addHandler(Events.API_SEARCH_RESPONSE, function(response) { 
      var hasTimeIntervalChanged = response.diff.hasOwnProperty('from') || response.diff.hasOwnProperty('to');
      
      // If this search was a change to the time interval, we want to keep all our markers on the map
      if (hasTimeIntervalChanged) {
        update(response.items, true);
      } else {
        clear();
        update(response.items);
      }
        
      // setTimeout(fitToObjects, 1);              
    });        
    
    // We want to know about user-issued queries, because as long
    // as a user query is 'active', we don't want to add/remove
    /* stuff from the map
    eventBroker.addHandler(Events.SEARCH_CHANGED, function(change) {
      console.log(change);

      if (change.query) {
        if (pendingQuery !== change.query) // New query - clear the map
          clear();
        
        pendingQuery = change.query;
      } else  if (change.hasOwnProperty('query')) { // If the change removed the query, clear the map
        clear();
      }
    });
    */
        
    
    /*
    eventBroker.addHandler(Events.MOUSE_OVER_RESULT, function(result) {
      if (result) {
        if (exists(result.identifier))
          highlight(result.identifier);
      } else {
        highlight();
      }
    });
    */
    
    /*
    eventBroker.addHandler(Events.SELECT_RESULT, function(result) {
      if (result) {
        var tuple = select(result.identifier),
            markerLatLng = (tuple) ? tuple.marker.getBounds().getCenter() : false;
            
        if (markerLatLng && !map.getBounds().contains(markerLatLng))
          map.panTo(markerLatLng);
      } else {
        highlight();
      }
    });
    */
  };
  
  return ObjectLayer;
  
});
