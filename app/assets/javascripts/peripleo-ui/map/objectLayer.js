define(['common/hasEvents', 'peripleo-ui/events/events'], function(HasEvents, Events) {
  
  var TOUCH_DISTANCE_THRESHOLD = 18,
  
      SIZE_LARGE = 8,
      
      SIZE_MEDIUM = 4,
      
      SIZE_SMALL = 3,
  
      BASE_STYLE = {
        color: '#a64a40',
        opacity: 1,
        fillColor: '#e75444',
        fillOpacity: 1,
        weight:1.5,
        radius: SIZE_MEDIUM
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
         
    var self = this,
        
        /** Feature group for polygon overlays **/           
        shapeFeatures = L.featureGroup().addTo(map),
    
        /** Feature group for point overlays **/   
        pointFeatures = L.featureGroup().addTo(map),
        
        /** Map[id -> (object, marker)] to support 'findById'-type queries **/
        objectIndex = {},
        
        /** Map[geometryHash -> (marker, Array<object>)] to support 'findByGeometryHash'-type queries **/
        markerIndex = {},
        
        /** An object { marker: CircleMarker, pin: Marker, objects: Array<object> } **/        
        currentSelection = false,
        
        /** A pin marker for temporary emphasis - e.g. while the mouse hovers over a search result in the list **/
        emphasisPin = false,
        
        /** Flag indicating whether the UI is in subsearch state **/
        isStateSubsearch = false,
        
        /** Flag indicating whether the user has stopped exploration mode **/
        stoppedExplorationMode = false,
        
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
        
        /** Shorthand for removing select & emphasis markers **/
        deselect = function() {
          if (currentSelection)
            map.removeLayer(currentSelection.pin);
          
          if (emphasisPin)
            map.removeLayer(emphasisPin);
        },
        
        /** Selects the marker with the specified geometry hash **/
        selectByGeomHash = function(geomHash) {  
          var tuple = markerIndex[geomHash],
              marker, center, pin;

          deselect();
          
          if (tuple) {
            marker = tuple._1;  
            center = marker.getBounds().getCenter();
            pin = L.marker(center).addTo(map);
            
            currentSelection = { marker: marker, pin: pin, objects: tuple._2 };
            
            eventBroker.fireEvent(Events.SELECT_MARKER, tuple._2);
          } else {
            clearSelection();
          }
        },  
        
        /** 
         * Programmatically selects a (set of) object(s)
         * 
         * TODO support multi-select
         * 
         */
        selectObjects = function(objects) {
          var tuple = (objects) ? markerIndex[createGeometryHash(objects[0].geometry)] : false,
              marker, center, pin;
          
          deselect();
                    
          if (tuple) {
            marker = tuple._1;  
            center = marker.getBounds().getCenter();
            pin = L.marker(center).addTo(map);
            
            currentSelection = { marker: marker, pin: pin, objects: tuple._2 };

            self.fireEvent('highlight', marker.getBounds());
          } else {            
            
            // TODO create marker and select
          
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
        },
        
        /** Helper that finds the marker for the specified object and emphasizes it **/
        emphasiseObject = function(object) {
          var tuple, center;
          
          // TODO should we also check if the emphasis is on the current selection (and change marker color, etc.?)
          
          if (object && object.geometry) {
            tuple = markerIndex[createGeometryHash(object.geometry)];
            
            if (tuple) {
              // Object is already on the map - just add marker
              center = tuple._1.getBounds().getCenter();
              emphasisPin = L.marker(center).addTo(map);
              
              // TODO If it's a polygon -> change style!
              
            } else {
              // Object not yet on map - add it temporarily
              emphasisPin = L.geoJson(object.geometry, Styles.POLY_EMPHASIS).addTo(map);
            }
          } else if (emphasisPin) {
             // No object or object without geometry - remove emphasis
             map.removeLayer(emphasisPin);
          }
        },
        
        /** Clears all objects from the map, except the selection **/
        clearMap = function() {  
          var selectedGeometry, selectedMarker;
             
          pointFeatures.clearLayers();          
          shapeFeatures.clearLayers();
          objectIndex = {};
          markerIndex = {};
          
          // Retain selection if any
          if (currentSelection) {
            // Add selection to map
            selectedGeometry = currentSelection.objects[0].geometry;
            selectedMarker = currentSelection.marker;
            
            if (selectedGeometry.type === 'Point')
              selectedMarker.addTo(pointFeatures);
            else
              selectedMarker.addTo(shapeFeatures);
              
            // Add selection to object index
            jQuery.each(currentSelection.objects, function(idx, obj) {
              objectIndex[obj.identifier] = { _1: obj, _2: selectedMarker };
            });
            
            // Add selection to marker index
            markerIndex[createGeometryHash(selectedGeometry)] = 
              { _1: selectedMarker, _2: currentSelection.objects };
          }
        },
        
        /** Clears the current selection **/
        clearSelection = function() {
          if (currentSelection) {
            map.removeLayer(currentSelection.pin);
            currentSelection = false;
            eventBroker.fireEvent(Events.SELECT_MARKER, false);
          }
        },
        
        /** Updates the object layer with a new search response or view update **/
        update = function(objects) {
          var maxCount = objects[0].result_count, // Results come sorted by count
              upperLimit = Math.round(0.8 * maxCount), // Upper 20% will be big
     
              setSize = function(marker, resultCount) {
                if (upperLimit > 1) {
                  if (resultCount > upperLimit)
                    marker.setRadius(SIZE_LARGE);
                  else if (resultCount < 2)
                    marker.setRadius(SIZE_SMALL);
                }
              };
          
          jQuery.each(objects.reverse(), function(idx, obj) {
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
                  setSize(existingObjectTuple._2, obj.result_count);

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
                    setSize(marker, obj.result_count);
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
        };

    map.on('click', function(e) { 
      selectNearest(e.latlng, TOUCH_DISTANCE_THRESHOLD); 
    });

    eventBroker.addHandler(Events.API_VIEW_UPDATE, function(response) {
      // IxD policy: we only show markers if there's a query or if we're in exploration mode
      if (response.params.query || response.exploration_mode)
        if (response.top_places.length > 0)
          update(response.top_places);
    });
        
    eventBroker.addHandler(Events.API_SEARCH_RESPONSE, function(response) { 
      // Always clear on search response, but leave drawing up to API_VIEW_UPDATE method
      clearMap();

      // IxD policy: if there's a new query phrase, we want to map to re-fit automatically, 
      // unless: (i) we're in exploration mode, or (ii) we just left exploration mode.
      // Also, we place the fit at the end of the execution loop, so it happens after
      // the API_VIEW_UPDATE handler, which does the drawing.
      if (response.diff.query && !response.exploration_mode) {
        if (stoppedExplorationMode)
          stoppedExplorationMode = false;
        else
          setTimeout(fitToObjects, 1);
      }
    });        

    eventBroker.addHandler(Events.MOUSE_OVER_RESULT, emphasiseObject);
    eventBroker.addHandler(Events.SELECT_RESULT, selectObjects);
    
    // Track state changes
    eventBroker.addHandler(Events.TO_STATE_SUB_SEARCH, function() {
      isStateSubsearch = true;
    });
    eventBroker.addHandler(Events.TO_STATE_SEARCH, function() {
      isStateSubsearch = false;
    });
    eventBroker.addHandler(Events.STOP_EXPLORATION, function() {
      stoppedExplorationMode = true;
    });
    
    HasEvents.apply(this);
  };
  
  ObjectLayer.prototype = Object.create(HasEvents.prototype);
  
  return ObjectLayer;
  
});
