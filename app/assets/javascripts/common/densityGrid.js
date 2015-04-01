define(function() {
  
  var densityGridLayer = function() {

    var isHidden = false,
    
        render = function(canvasOverlay, params) {          
          if (!params.options.heatmap)
            return;

          var ctx = params.canvas.getContext('2d');
          ctx.clearRect(0, 0, params.canvas.width, params.canvas.height);
          ctx.fillStyle = '#5254a3';
          ctx.strokeStyle = '#5254a3';
          
          // Hack!
          var heatmap = params.options.heatmap,
              weights = jQuery.map(heatmap.cells, function(val) { return val.weight; });
              mean = weights.reduce(function(a, b) { return a + b; }, 0) / weights.length,
              maxWeight = heatmap.max_value,
              
              xyOrigin = canvasOverlay._map.latLngToContainerPoint([0, 0]),
              xyOneCell = canvasOverlay._map.latLngToContainerPoint([1.40625, 1.40625]), // TODO grab info from heatmap JSON
              cellHalfDimensions = { x: (xyOneCell.x - xyOrigin.x) / 2, y: (xyOrigin.y - xyOneCell.y) / 2 };
          
          var classified = jQuery.map(heatmap.cells, function(val) {
            return { c: val, is_outlier: val.weight > 5 * mean };
          });
          
          jQuery.each(classified, function(idx, tuple) {
            var x = tuple.c.x, y = tuple.c.y,
                delta = Math.sqrt(tuple.c.weight / (5 * mean), 2),
                bottomLeft = canvasOverlay._map.latLngToContainerPoint([y + heatmap.cell_height / 2, x - heatmap.cell_width / 2]);
                topRight = canvasOverlay._map.latLngToContainerPoint([y - heatmap.cell_height / 2, x + heatmap.cell_width / 2]);
                width = topRight.x - bottomLeft.x,
                height = topRight.y - bottomLeft.y;
              
              if (tuple.is_outlier)
                ctx.globalAlpha = 0.9;    
              else
                ctx.globalAlpha = 0.1 + delta * 0.7;  
              ctx.fillRect(bottomLeft.x, bottomLeft.y, width, height);
              ctx.globalAlpha = 0.2;
              ctx.strokeRect(bottomLeft.x, bottomLeft.y, width, height);          
          });
        },
        
        canvasOverlay = L.canvasOverlay().drawing(render);

    /** Privileged methods **/            
    this.update = function(heatmap) {
      canvasOverlay.params({ heatmap: heatmap });
      canvasOverlay.redraw();
    };
    
    this.hideasdf = function() {
      if (!isHidden) {
        map.removeLayer(canvasOverlay);
        isHidden = true;
      }
    };
    
    this.showasdf = function() {
      if (isHidden) {
        map.addLayer(canvasOverlay);
        isHidden = false;
      }
    };
    
    this.adsaddTo = function(map) {
      canvasOverlay.addTo(map);
      return this; // Just to mimick with Leaflet's API
    };
    
  };
  
  return densityGridLayer;
  
});
