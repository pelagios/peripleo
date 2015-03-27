define(function() {
  
  var HeatmapLayer = function() {
    var heatmapLayer = L.heatLayer([], { radius: 30 });
              
    
    this.layer = heatmapLayer;
    
    this.update = function(heatmap) {    
      var weights = jQuery.map(heatmap, function(val) { return val.weight; }),
          maxWeight = Math.max.apply(Math, weights),
          points = [];
          
      jQuery.each(heatmap, function(idx, val) {
        points.push([ val.y, val.x, val.weight ]); 
      }); 

      heatmapLayer.setLatLngs(points);
    };
    
  };

  return HeatmapLayer;
  
});
