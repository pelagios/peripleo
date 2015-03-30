define(function() {
  
  /** Constants **/
  var  BAR_STROKE = '#3182bd',
        
       BAR_FILL = '#6baed6';
       

  var TimeHistogram = function(divId, onIntervalChanged) {
    
        /** Container DIV **/
    var container = jQuery('#' + divId),
    
        /** Canvas element **/
        canvas = container.find('canvas'),
        
        /** Drawing context **/
        ctx = canvas[0].getContext('2d'),
        
        /** Interval handle elements **/
        fromHandle = container.find('.handle.from'),
        fromHandleLabel = fromHandle.find('.label'),
        
        toHandle = container.find('.handle.to'),
        toHandleLabel = toHandle.find('.label'),
        
        /** Handle width / 2 - it's safe to assume that both handles are identical **/
        handleOffset = fromHandle.outerWidth() / 2,
        
        /** Interval label elements **/
        fromLabel = container.find('.label.from'),
        toLabel = container.find('.label.to'),
        
        /** Buffers the current time  **/
        timeRange = { from: 0, to: 0 },
        
        /** Helper function to make a DOM element draggable along X-axis, within offset limits **/
        makeXDraggable = function(element, dragCallback, stopCallback) {
          element.draggable({ 
            axis: 'x', 
            containment: 'parent', // TODO proper containment?
            drag: dragCallback,
            stop: stopCallback
          });
        },
        
        resetHandles = function() {
          var canvasOffset = canvas.position().left;
          fromHandle.css('left', canvasOffset - handleOffset);
          toHandle.css('left', canvas[0].width + canvasOffset - handleOffset);
        },
        
        /** Formats an integer year for screen display **/
        formatYear = function(year) { if (year < 0) return -year + ' BC'; else return year + ' AD'; },
        
        /** Converts an X offset on the scale to a year **/
        xToYear = function(x) {
          var duration = timeRange.to - timeRange.from,
              yearsPerPixel = duration / canvas[0].width;
              
          return Math.round(timeRange.from + x * yearsPerPixel);          
        },
        
        getSelectedRange = function() {
          var updatedRange,
              xFrom = fromHandle.position().left + handleOffset - canvas.position().left,
              yearFrom = xToYear(xFrom),
              
              xTo = toHandle.position().left + handleOffset - canvas.position().left,
              yearTo = xToYear(xTo);
              
          return { from: yearFrom, to: yearTo };
        },
        
        onDrag = function(e) {
          if (e.target === fromHandle[0])
            fromHandleLabel.html(formatYear(getSelectedRange().from));
          else
            toHandleLabel.html(formatYear(getSelectedRange().to));
        },
        
        onDragStop = function() {
          fromHandleLabel.empty();
          toHandleLabel.empty();
          if (onIntervalChanged)
            onIntervalChanged(getSelectedRange());
        };
        
    /** Privileged methods **/
    
    this.update = function(values) {
      if (values.length > 0) {                      
        var maxValue = Math.max.apply(Math, jQuery.map(values, function(value) { return value.val; })),
            fromYear = values[0].year,
            toYear = values[values.length - 1].year,
            width = ctx.canvas.width,
            height = ctx.canvas.height,
            xOffset = 0;
              
        ctx.clearRect (0, 0, canvas[0].width, canvas[0].height);
        resetHandles();
        
        jQuery.each(values, function(idx, value) {
          var barHeight = Math.round(value.val / maxValue * 100);             
          
          ctx.strokeStyle = BAR_STROKE;
          ctx.fillStyle = BAR_FILL;
          ctx.beginPath();
          ctx.rect(xOffset + 0.5, height - barHeight - 9.5, 4, barHeight);
          ctx.fill();
          ctx.stroke();
          xOffset += 7;
        });
          
        timeRange.from = fromYear;
        timeRange.to = toYear;
        
        fromLabel.html(formatYear(fromYear));
        toLabel.html(formatYear(toYear));
      };
    };
    
    // Initialize interval drag handles
    makeXDraggable(fromHandle, onDrag, onDragStop);
    makeXDraggable(toHandle, onDrag, onDragStop);
  };
  
  return TimeHistogram;

});
