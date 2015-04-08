/** The base map **/
define(['search/map/densityGridLayer', 'search/map/placeLayer', 'search/events'], function(DensityGrid, PlaceLayer, Events) {
  
  var Map = function(div, eventBroker) {  
    var Layers = {
      
          DARE : L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
                   attribution: 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>',
                   minZoom:3,
                   maxZoom:11
                 }), 
                 
          AWMC : L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
                   attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                     'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                     'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                     '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
                 })
                 
        },    
        
        /** Base Layer dictionary **/
        baseLayers = 
          { 'Bing Satellite': Layers.BingSatellite, 
            'Bing Road': Layers.BingRoad,
            'OpenStreetMap': Layers.OSM,
            'Empty Base Map (<a href="http://awmc.unc.edu/wordpress/tiles/map-tile-information" target="_blank">AWMC</a>)': Layers.AWMC, 
            'Roman Empire Base Map (<a href="http://imperium.ahlfeldt.se/" target="_blank">DARE</a>)': Layers.DARE },
        
        /** Map **/    
        map = new L.Map(div, {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 3,
          zoomControl: false,
          layers: [ Layers.AWMC ]
        }),
                
        // densityGrid = new DensityGrid().addTo(map),
        
        placeLayer = new PlaceLayer(map, eventBroker),
        
        getBounds = function() {
          var b = map.getBounds(),
              w = (b.getWest() < -180) ? -180 : b.getWest(),
              e = (b.getEast() > 180) ? 180 : b.getEast(),
              s = (b.getSouth() < -90) ? -90 : b.getSouth(),
              n = (b.getNorth() > 90) ? 90 : b.getNorth();
              
          return { north: n, east: e, south: s, west: w };
        };
       
    eventBroker.addHandler(Events.UPATED_COUNTS, function(response) {
      placeLayer.setItems(response.items);
    });
    
    /** Obviously, we listen for new heatmaps **/
    eventBroker.addHandler(Events.UPDATED_HEATMAP, function(heatmap) {
      // densityGrid.update(heatmap);
      if (heatmap.top_places) {
        placeLayer.setPlaces(heatmap.top_places);
      }
    });
    
    eventBroker.addHandler(Events.HOVER_RESULT, function(result) {
      placeLayer.showItem(result);
    });
    
    /** Request an updated heatmap on every moveend **/
    map.on('moveend', function() {
      eventBroker.fireEvent(Events.REQUEST_UPDATED_HEATMAP, getBounds());
    });
    
    /** Request count & histogram updates on every move **/
    map.on('move', function() {
      eventBroker.fireEvent(Events.REQUEST_UPDATED_COUNTS, getBounds());
    });
    
    map.on('click', function(e) {
      placeLayer.clickNearest(e.latlng);
    });
    
    this.getBounds = getBounds;
    
  };
  
  return Map;
  
});
