define(['peripleo-ui/events/events'], function(Events) {
  
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
    
        POINT_RED: (function() { return jQuery.extend({}, BASE_STYLE); })(),
        
        POINT_GREY: (function() { 
          var style = jQuery.extend({}, BASE_STYLE);
          style.color = '#959595';
          style.fillColor = '#aeaeae';
          return style;
        })(),
        
        POLY_RED: (function() { 
          var style = jQuery.extend({}, BASE_STYLE);
          style.color = '#db473a';
          style.fillColor = '#db473a';
          style.fillOpacity = 0.12;
          style.weight = 0.75;
          return style;
        })(),
        
        POLY_EMPHASIS: (function() { 
          var style = jQuery.extend({}, BASE_STYLE);
          style.color = '#C28A29';
          style.fillColor = '#C28A29';
          style.fillOpacity = 0.12;
          style.weight = 0.75;
          return style;
        })(),
        
        POLY_GREY: (function() { 
          var style = jQuery.extend({}, BASE_STYLE);
          style.color = '#959595';
          style.fillColor = '#aeaeae';
          style.fillOpacity = 0.12;
          style.weight = 0.75;
          return style;
        })()     
                
      };
      
  var ObjectLayer = function(map, eventBroker) {
    
        /** Feature group for polygon overlays **/           
    var shapeFeatures = L.featureGroup().addTo(map),
    
        /** Feature group for point overlays **/   
        pointFeatures = L.featureGroup().addTo(map),
        
        /** Map[id -> (object, marker)] to support 'findById'-type queries **/
        objectIndex = {},
        
        /** Map[geometryHash -> (marker, Array<object>)] to support 'findByGeometryHash'-type queries **/
        markerIndex = {},
        
        /** A tuple (marker, Array<object>) **/        
        currentSelection = false,
        
        /** The map pin highlighting the currently emphasised marker **/
        emphasisPin = false,
        
        /** TODO is this a good solution? **/
        temporaryEmphasis = false,
        
        /** Flag indicating whether the UI is in subsearch state **/
        isStateSubsearch = false,
        
        /** 
         * Creates a string representation of a GeoJSON geometry to be used as a
         * key in the marker index. (The only requirements are that the representation
         * is unique for every possible geometry, and that identical geometries
         * will result in the same representation.)
         */
        createGeometryHash = function(geometry) {
          return JSON.stringify(geometry);
        },
        
        /**
         * Returns the marker corresponding to the geometry of the specified object.
         * 
         * Since the method works based on geometry rather than ID, it will return
         * correct markers for places as well as objects related to them.
         */
        getMarkerForObject = function(object) {
          if (object && object.geometry) {
            var tuple = markerIndex[createGeometryHash(object.geometry)];
            if (tuple)
              return tuple._1;
          }
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
          if (invalidateMarkers) {
            pointFeatures.setStyle(Styles.POINT_GREY);
            shapeFeatures.setStyle(Styles.POLY_GREY);
          }
          
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
                
                if (existingObjectTuple._1.geometry.type === 'Point')
                  existingObjectTuple._2.setStyle(Styles.POINT_RED);
                else
                  existingObjectTuple._2.setStyle(Styles.POLY_RED);

                existingObjectTuple._2.bringToFront();
              } else {                  
                if (existingMarkerTuple) { // There's a marker at that location already - add the object
                  existingMarkerTuple._2.push(obj); 
                  marker = existingMarkerTuple._1;
                  marker.setStyle(Styles.SMALL);
                  marker.bringToFront();
                } else { // Create and add a new marker
                  type = obj.geometry.type;
                  if (type === 'Point') {
                    marker = L.circleMarker([obj.geo_bounds.max_lat, obj.geo_bounds.max_lon], Styles.POINT_RED);
                    marker.addTo(pointFeatures); 
                  } else {
                    marker = L.geoJson(obj.geometry, Styles.POLY_RED);
                    marker.addTo(shapeFeatures); 
                  }
          
                  marker.on('click', function(e) { selectByGeomHash(geomHash); });
                  markerIndex[geomHash] = { _1: marker, _2: [obj] };
                }

                objectIndex[id] = { _1: obj, _2: marker };
              }
            }
          });
        },
        
        /** Returns the current layer bounds, merging point and shape layer bounds **/
        getLayerBounds = function() {
          var pointBounds = pointFeatures.getBounds(),
              pointBoundsValid = pointBounds.isValid(),
              shapeBounds = shapeFeatures.getBounds(),
              shapeBoundValid = shapeBounds.isValid(),
              mergedBounds;
              
          if (pointBoundsValid && shapeBoundValid) {
            mergedBounds = pointBounds;
            mergedBounds.extend(shapeBounds);
            return mergedBounds;
          } else if (pointBoundsValid) {
            return pointBounds;
          } else if (shapeBoundValid) {
            return shapeBounds;
          } else {
            // Doesn't matter, as long as we return invalid bounds
            return pointBounds;
          }
        },
        
        /** Helper method that resets map location and zoom to fit all current objects **/
        fitToObjects = function() {
          var bounds;
          
          if (!jQuery.isEmptyObject(markerIndex)) {
            bounds = getLayerBounds();
            
            if (bounds.isValid()) {
              map.fitBounds(bounds, {
                animate: true,
                paddingTopLeft: [380, 20],
                paddingBottomRight: [20, 20],
                maxZoom: 9
              });
            }
          }
        },
        
        /** Clears all ojbects from the map **/
        clear = function() {
          clearSelection();
          pointFeatures.clearLayers();          
          shapeFeatures.clearLayers();
          objectIndex = {};
          markerIndex = {};
        },
        
        /** Function that emphasises the marker passed to it **/
        emphasiseMarker = function(marker) {  
          var markerToHighlight = (marker) ? marker : (currentSelection) ? currentSelection._1 : false,
              latlon;
                  
          if (emphasisPin) {
            map.removeLayer(emphasisPin);
            emphasisPin = false;
          }
          
          if (markerToHighlight) {
            latlon = markerToHighlight.getBounds().getCenter();
            emphasisPin = L.marker(latlon).addTo(map);
          }   
        },
        
        /**
         * Helper that finds the marker for the specified object and emphasizes it.
         * 
         * Returns the marker that was emphasized, if any.
         */
        emphasiseObject = function(object) {
          var tuple, geomHash;
          if (object && object.geometry) {
            geomHash = createGeometryHash(object.geometry);
            tuple = markerIndex[geomHash];
            if (tuple) {
              emphasiseMarker(tuple._1);
              return tuple._1;
            } else {

              // TODO turn this into a clean solution!
              
              temporaryEmphasis = L.geoJson(object.geometry, Styles.POLY_EMPHASIS);
              temporaryEmphasis.addTo(map);
              
            }
          } else { // No object or object without geometry- de-emphasize
            emphasiseMarker();
            
            // TODO hack
            if (temporaryEmphasis) {
              map.removeLayer(temporaryEmphasis);
            }
          }
        },
        
        /** Selects (and emphasises) the marker with the specified geometry hash **/
        selectByGeomHash = function(geomHash) {    
          currentSelection = markerIndex[geomHash]; // (marker, Array<object>)
          if (currentSelection) {
            emphasiseMarker(currentSelection._1); 
            eventBroker.fireEvent(Events.SELECT_MARKER, currentSelection._2);
          }
        },
        
        /** Clears the current selection & emphasis **/
        clearSelection = function() {
          if (currentSelection) {
            currentSelection = false;
            emphasiseMarker();
            eventBroker.fireEvent(Events.SELECT_MARKER, false);
          }
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
     
    // TODO use click->select nearest only on touch devices?
    map.on('click', function(e) { 
      selectNearest(e.latlng, TOUCH_DISTANCE_THRESHOLD); 
    });
    
    eventBroker.addHandler(Events.TO_STATE_SUB_SEARCH, function() {
      isStateSubsearch = true;
    });
    
    eventBroker.addHandler(Events.TO_STATE_SEARCH, function() {
      isStateSubsearch = false;
    });

    eventBroker.addHandler(Events.API_VIEW_UPDATE, function(response) {
      /*
      var  hasTimeIntervalChanged = 
        (response.diff) ? response.diff.hasOwnProperty('from') || response.diff.hasOwnProperty('to') : false;
      
      // 'IxD policy': if the time interval changed, we want to grey-out all markers that are
      // not top places in this response
      if (!isStateSubsearch) {
        if (!hasTimeIntervalChanged)
          clear();
          
        update(response.top_places, hasTimeIntervalChanged);
      }
      */
      
      // TODO clean up
      
      // Trial IxD policy: we don't add top places, unless there's currently a search query
      if (response.params && response.params.query)
        update(response.top_places);
    });
        
    eventBroker.addHandler(Events.API_SEARCH_RESPONSE, function(response) { 
      // 'IxD policy': if the user submitted a new query phrase (or cleared the current one), we want
      // to clear the map; in case of a new query phrase, we also want to fit the view area to the results
      if (response.diff.hasOwnProperty('query')) {
        clear();
        if (response.diff.query)
          setTimeout(fitToObjects, 1);      
      }     
    });        

    eventBroker.addHandler(Events.MOUSE_OVER_RESULT, emphasiseObject);
    
    eventBroker.addHandler(Events.SELECT_RESULT, function(result) {
      var marker = emphasiseObject(result[0]),
          latlng;
          
      if (marker) {
        currentSelection = { _1: marker , _2: [ result ] };
        latlng = marker.getBounds().getCenter();
        
        if (!map.getBounds().contains(latlng))
          map.panTo(latlng);
      }
    });
  };
  
  return ObjectLayer;
  
});
