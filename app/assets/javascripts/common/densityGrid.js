define(function() {
  
  var densityGridLayer = function() {
    var render = function(canvasOverlay, params) {          
          if (!params.options.cells)
            return;
            
          var ctx = params.canvas.getContext('2d');
          ctx.clearRect(0, 0, params.canvas.width, params.canvas.height);
          ctx.fillStyle = '#d62728';
          
          // Hack!
          var weights = jQuery.map(params.options.cells, function(tuple) { return tuple.weight; }),
              maxWeight = Math.max.apply(Math, weights),
              minWeight = Math.min.apply(Math, weights);
          
          
          jQuery.each(params.options.cells, function(idx, tuple) {
            var x = tuple.x, y = tuple.y,
                delta = Math.sqrt((tuple.weight - minWeight) / (maxWeight - minWeight));
                
            if (params.bounds.contains([y, x])) {
              dot = canvasOverlay._map.latLngToContainerPoint([y, x]);
              ctx.globalAlpha = 0.3 + delta * 0.7;
              ctx.beginPath();
              ctx.arc(dot.x, dot.y, 5, 0, Math.PI * 2);
              ctx.fill();
              ctx.closePath();              
            }
          });
        },
        
        canvasOverlay = L.canvasOverlay().drawing(render);

    /** Privileged methods **/        
    this.addTo = function(map) {
      canvasOverlay.addTo(map);
      return this; // Just to mimick with Leaflet's API
    };
    
    this.update = function(heatmap) {
      canvasOverlay.params({ cells: heatmap });
      canvasOverlay.redraw();
    };
  };
  
  return densityGridLayer;
  
});
