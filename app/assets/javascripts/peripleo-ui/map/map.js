/** The base map **/
define(['peripleo-ui/events/events',
        'peripleo-ui/map/objectLayer',
        'peripleo-ui/map/resultBBoxLayer',
        'peripleo-ui/map/densityGrid'], function(Events, ObjectLayer, BBoxLayer, DensityGrid) {

  var Map = function(div, eventBroker) {

        /** Map layers **/
    var Layers = {

          dare : L.tileLayer('http://pelagios.org/tilesets/imperium/{z}/{x}/{y}.png', {
                   attribution: 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>',
                   minZoom:3,
                   maxZoom:11
                 }),

          awmc : L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
                   attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                     'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                     'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                     '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
                 }),

          osm  : L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
	                 attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
                 }),

          satellite : L.tileLayer('http://api.tiles.mapbox.com/v4/mapbox.satellite/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoibWFwYm94IiwiYSI6IlhHVkZmaW8ifQ.hAMX5hSW-QnTeRCMAy9A8Q', {
	                 attribution: '<a href="https://www.mapbox.com/about/maps/">&copy; Mapbox</a> <a href="http://www.openstreetmap.org/about/">&copy; OpenStreetMap</a>'
                 })

        },

        /** Default "closeup" zoom levels per layer **/
        closeupZoom = { dare: 11, awmc: 12, osm: 15, satellite: 17 },

        /** To keep track of current layer **/
        currentLayer = { name: 'awmc', layer: Layers.awmc },

        /** Map **/
        map = new L.Map(div, {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 3,
          zoomControl: false,
          layers: [ currentLayer.layer ]
        }),

        // objectLayer = new ObjectLayer(map, eventBroker),

        bboxLayer = new BBoxLayer(map, eventBroker),

        densityGrid = new DensityGrid(map, eventBroker),

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
            map.removeLayer(currentLayer.layer);
            currentLayer = { name: name, layer: layerToShow };
            map.addLayer(currentLayer.layer);
          }
        },

        zoomTo = function(bounds) {
          var center = bounds.getCenter();
          if (!map.getBounds().contains(center))
            map.panTo(center);

          map.fitBounds(bounds, { maxZoom: closeupZoom[currentLayer.name] });
        };

    /** Request count & histogram updates on every move **/
    map.on('move', function() {
      eventBroker.fireEvent(Events.VIEW_CHANGED, getBounds());
    });

    // objectLayer.on('highlight', zoomTo);

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
