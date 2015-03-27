define(function() {
  
  var densityGridLayer = function() {
    var render = function(canvasOverlay, params) {          
          if (!params.options.heatmap)
            return;
            
          var ctx = params.canvas.getContext('2d');
          ctx.clearRect(0, 0, params.canvas.width, params.canvas.height);
          ctx.fillStyle = '#5254a3';
          ctx.strokeStyle = '#5254a3';
          
          // Hack!
          var heatmap = params.options.heatmap,
              maxWeight = heatmap.max_value,
              xyOrigin = canvasOverlay._map.latLngToContainerPoint([0, 0]),
              xyOneCell = canvasOverlay._map.latLngToContainerPoint([1.40625, 1.40625]), // TODO grab info from heatmap JSON
              cellHalfDimensions = { x: (xyOneCell.x - xyOrigin.x) / 2, y: (xyOrigin.y - xyOneCell.y) / 2 };
              
    
              
          jQuery.each(heatmap.cells, function(idx, tuple) {
            var x = tuple.x, y = tuple.y,
                delta = Math.sqrt(tuple.weight / maxWeight);

              dot = canvasOverlay._map.latLngToContainerPoint([y, x]);
              offsetOne = canvasOverlay._map.latLngToContainerPoint([y + heatmap.cell_height, x + heatmap.cell_width]),
              width = Math.ceil(offsetOne.x - dot.x),
              height = Math.ceil(dot.y - offsetOne.y);
              
              var x = Math.round(dot.x - width / 2),
                  y = Math.round(dot.y - height / 2);
              
              ctx.globalAlpha = 0.3 + delta * 0.7;    
              ctx.fillRect(x, y, width, height);
              ctx.globalAlpha = 0.7;
              ctx.strokeRect(x, y, width, height);          
          });
        },
        
        canvasOverlay = L.canvasOverlay().drawing(render);

    /** Privileged methods **/        
    this.addTo = function(map) {
      canvasOverlay.addTo(map);
      return this; // Just to mimick with Leaflet's API
    };
    
    this.update = function(heatmap) {
      canvasOverlay.params({ heatmap: heatmap });
      canvasOverlay.redraw();
    };
  };
  
  return densityGridLayer;
  
});
