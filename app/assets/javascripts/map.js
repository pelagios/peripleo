require([], function() {
  
  jQuery(document).ready(function() {
    var resultStats = jQuery('#result-stats'), 
    
        awmcLayer = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Data &copy; <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
          '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC</a>'}),    
          
        typeChartCtx = document.getElementById('type-chart').getContext('2d'),
        typeChartData = [{
            value: 300,
            color:"#F7464A",
            highlight: "#FF5A5E",
            label: "Place"
          },{
            value: 50,
            color: "#46BFBD",
            highlight: "#5AD3D1",
            label: "Item"
          }],
        typeChart = new Chart(typeChartCtx).Pie(typeChartData, { percentageInnerCutout: 50, animationSteps : 10, animationEasing: false });
        
        sourceChartCtx = document.getElementById('source-chart').getContext('2d'),
        sourceChartData = [{
            value: 300,
            color:"#F7464A",
            highlight: "#FF5A5E",
            label: "Red"
          },{
            value: 50,
            color: "#46BFBD",
            highlight: "#5AD3D1",
            label: "Green"
          }],
        sourceChart = new Chart(sourceChartCtx).Pie(sourceChartData, { percentageInnerCutout: 50, animationSteps : 10, animationEasing: false });
         
        /*
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
        */
        
        map = new L.Map('map', {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 3,
          layers: [ awmcLayer],
          zoomControl:false
        }),
        
        pendingRequest = false,
        
        /*
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
            jQuery.getJSON('/api-v3/search?limit=1&bbox=' + bboxParam, function(response) {
              resultStats.html(numeral(response.total).format('0,0') + ' Results');
              updateHeatmap(response.heatmap);              
            });            
        };
        */
        
        updateFacets = function(facets) {         
          var typeDim = jQuery.grep(facets, function(facet) {
                return facet.dimension === 'type';
              }),
              
              updatedTypeData = (typeDim.length > 0) ? jQuery.map(typeDim[0].top_children, function(val) {
                return { value: val.count, label: val.label, highlight: "#FF5A5E", label: "Red" };  
              }) : false,
              
              sourceDim = jQuery.grep(facets, function(facet) {
                return facet.dimension === 'dataset';
              }),
              
              updatedSourceData = (typeDim.length > 0) ? jQuery.map(typeDim[0].top_children, function(val) {
                return { value: val.count, label: val.label, highlight: "#FF5A5E", label: "Red" };  
              }) : false;

          jQuery.each(updatedTypeData, function(idx, val) {
            typeChart.segments[idx].value = val.value;
          });
          
          typeChart.update();
        },
        
        updateCount = function(e) {
          var b = map.getBounds(),
              bboxParam = b.getWest() + ',' + b.getEast() + ',' + b.getSouth() + ',' + b.getNorth();
          
          if (!pendingRequest) {    
            pendingRequest = true;
            jQuery.getJSON('/api-v3/search?facets=true&bbox=' + bboxParam, function(response) {
              resultStats.html(numeral(response.total).format('0,0') + ' Results');   
              updateFacets(response.facets);      
            })
            .always(function() {
              pendingRequest = false;
            });
          }
        };

        
    map.on('move', updateCount);
    updateCount();
        
  });
  
});
