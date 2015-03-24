require([], function() {
  
  jQuery(document).ready(function() {
    var resultStats = jQuery('#result-stats'), 
    
        awmcLayer = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Data &copy; <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
          '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC</a>'}),    
          
        // debugHeatmapLayer = L.layerGroup(),
          
        heatmapConfig = {
          radius: 24,
          useLocalExtrema:false,
          scaleRadius:false,
          maxOpacity:0.6,
          latField: 'y',
          lngField: 'x',
          blur:0.8,
          valueField: 'weight'
        },
        
        heatmapLayer = new HeatmapOverlay(heatmapConfig),
        
        map = new L.Map('map', {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 3,
          layers: [ awmcLayer, heatmapLayer ],
          zoomControl:false
        }),
        
        pendingRequest = false,
        
        updateHeatmap = function(points) {
          var maxWeight = 0;
                    
          // This is a bit ugly - any way around it?
          jQuery.each(points, function(idx, point) {
            point.weight = Math.sqrt(point.weight);
            if (point.weight > maxWeight)
              maxWeight = point.weight;
              
            
          });
          
          heatmapLayer.setData({
            max: maxWeight,
            data: points
          });
          
        },
        
        updateHM = function(e) {
        
          var b = map.getBounds(),
              bboxParam = b.getWest() + ',' + b.getEast() + ',' + b.getSouth() + ',' + b.getNorth();
          

            pendingRequest = true;
            jQuery.getJSON('/api-v3/search?limit=1&gazetteer=pleiades&heatmap=true&bbox=' + bboxParam, function(response) {
              resultStats.html(numeral(response.total).format('0,0') + ' Results');
              updateHeatmap(response.heatmap);              
            });
 
        };
        
        updateCount = function(e) {
          var b = map.getBounds(),
              bboxParam = b.getWest() + ',' + b.getEast() + ',' + b.getSouth() + ',' + b.getNorth();
          
          if (!pendingRequest) {    
            pendingRequest = true;
            jQuery.getJSON('/api-v3/search?limit=1&bbox=' + bboxParam, function(response) {
              resultStats.html(numeral(response.total).format('0,0') + ' Results');         
              pendingRequest = false;
            });
          }
        };
    
    /** Listen to all map changes **/    
    // map.on('move', updateCount);
    // map.on('moveend', updateHM);
        
  });
  
});
