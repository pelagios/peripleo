/** The base map **/
define(['peripleo-ui/events/events',
        'peripleo-ui/map/objectLayer',
        'peripleo-ui/map/densityGrid'], function(Events, ObjectLayer, DensityGrid) {

  var Map = function(div, eventBroker) {

        /** Map layers **/
    var Layers = {},

        /** Default "closeup" zoom levels per layer **/
        closeupZoom = {},

        /** To keep track of current layer **/
        currentLayer,

        /** Map **/
        map = new L.Map(div, {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 3,
          zoomControl: false
        }),

        objectLayer = new ObjectLayer(map, eventBroker),

        densityGrid = new DensityGrid(map, eventBroker),

        initLayers = function() {
          jQuery.getJSON('/peripleo/baselayers', function(data) {
            jQuery.each(data, function(idx, layer) {
              Layers[layer.name] = L.tileLayer(layer.path, layer.options);
              closeupZoom[layer.name] = layer.closeup_zoom;
            });

            if (currentLayer && currentLayer.name && Layers[currentLayer.name]) {
              // Pre-set layer from the URL bar, and it's in the list
              changeLayer(currentLayer.name);
            } else {
              // No pre-set layer - just use first from list
              changeLayer(data[0].name);
            }
          });
        },

        getBounds = function() {
          var b = map.getBounds(),
              w = (b.getWest() < -180) ? -180 : b.getWest(),
              e = (b.getEast() > 180) ? 180 : b.getEast(),
              s = (b.getSouth() < -90) ? -90 : b.getSouth(),
              n = (b.getNorth() > 90) ? 90 : b.getNorth();

          return { north: n, east: e, south: s, west: w, zoom: map.getZoom() };
        },

        /** JavaScript equality is by reference - we need to compare values **/
        boundsEqual = function(a, b) {
          if (a.north !== b.north)
            return false;
          if (a.east !== b.east)
            return false;
          if (a.south !== b.south)
            return false;
          if (a.west !== b.west)
            return false;

          return true;
        },

        changeLayer = function(name) {
          var layerToShow = Layers[name];
          if (layerToShow) {
            if (currentLayer && currentLayer.layer)
              map.removeLayer(currentLayer.layer);
            currentLayer = { name: name, layer: layerToShow };
            map.addLayer(currentLayer.layer);
          } else {
            // Hack!
            currentLayer = { name: name };
          }
        },

        zoomTo = function(bounds) {
          var center = bounds.getCenter();
          if (!map.getBounds().contains(center))
            map.panTo(center);

          map.fitBounds(bounds, { maxZoom: closeupZoom[currentLayer.name] });
        },

        toggleSampleRaster = function(show) {
          if (show)
            map.addLayer(Layers.sampleRaster);
          else
            map.removeLayer(Layers.sampleRaster);
        };

    initLayers();

    /** Request count & histogram updates on every move **/
    map.on('move', function() {
      eventBroker.fireEvent(Events.VIEW_CHANGED, getBounds());
    });

    objectLayer.on('highlight', zoomTo);

    eventBroker.addHandler(Events.LOAD, function(initialSettings) {
      var b = (initialSettings.bbox) ? initialSettings.bbox : getBounds();

      if (!boundsEqual(b, getBounds()))
        map.fitBounds([[b.south, b.west], [b.north, b.east]]);

      if (initialSettings.layer)
        changeLayer(initialSettings.layer);
    });

    eventBroker.addHandler(Events.CHANGE_LAYER, changeLayer);
    eventBroker.addHandler(Events.ZOOM_IN, function() { map.zoomIn(); });
    eventBroker.addHandler(Events.ZOOM_OUT, function() { map.zoomOut(); });

    eventBroker.addHandler(Events.TOGGLE_SAMPLE_RASTER, function(evt) {
      toggleSampleRaster(evt.enabled);
    });

    this.setView = function(center, zoom) {
      map.panTo(center);
      map.setZoom(zoom);
    };

    this.zoomTo = function(bbox) {
      var bounds =
        L.latLngBounds([[ bbox.min_lat, bbox.min_lon ],
                        [ bbox.max_lat, bbox.max_lon ]]);
      zoomTo(bounds);
    };
  };

  return Map;

});
