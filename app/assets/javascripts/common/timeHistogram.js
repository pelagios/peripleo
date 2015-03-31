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
        
        /** Interval bounds indicator **/s
        intervalBounds = container.find('.intervalbounds');
        
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
            containment: 'parent',
            drag: dragCallback,
            stop: stopCallback
          });
        },
        
        setFromHandle = function(x) {
          fromHandle.css('left', x - handleOffset);
          intervalBounds.css('left', x);
        },
        
        setToHandle = function(x) {
          toHandle.css('left', x - handleOffset);
          intervalBounds.css('width', x - fromHandle.position().left - handleOffset);
        },
        
        /** Make sure the handles are inside the canvas area **/
        resetHandles = function() {                 
          var canvasOffset = canvas.position().left,
              canvasWidth = canvas.outerWidth(),
              fromOffset = fromHandle.position().left,
              toOffset = toHandle.position().left;
              
          if (fromOffset < canvasOffset - handleOffset)
            setFromHandle(canvasOffset);
                    
          if (toOffset > canvasOffset + canvasWidth - handleOffset)
            setToHandle(canvasOffset + canvasWidth)
        },
        
        /** Formats an integer year for screen display **/
        formatYear = function(year) { if (year < 0) return -year + ' BC'; else return year + ' AD'; },
        
        /** Converts an X offset on the scale to a year **/
        xToYear = function(x) {
          var duration = timeRange.to - timeRange.from,
              yearsPerPixel = duration / canvas.outerWidth();
              
          return Math.round(timeRange.from + x * yearsPerPixel);          
        },
        
        yearToX = function(year) {
          var duration = timeRange.to - timeRange.from,
              pixelsPerYear = canvas.outerWidth() / duration;
              
          return Math.round((year - timeRange.from) * pixelsPerYear);
        },
        
        getSelectedRange = function() {
          var updatedRange,
              xFrom = fromHandle.position().left + handleOffset - canvas.position().left,
              yearFrom = xToYear(xFrom),
              
              xTo = toHandle.position().left + handleOffset - canvas.position().left,
              yearTo = xToYear(xTo);
              
          return { from: yearFrom, to: yearTo };
        },

        onDragHandle = function(e) {
          var maxX, minX, oppositeX,
              posX = jQuery(e.target).position().left + handleOffset;

          
          if (e.target === fromHandle[0]) {
            // Left handle constraint check
            minX = canvas.position().left;
            maxX = toHandle.position().left;
            
            if (posX < minX) {
              setFromHandle(minX);
              return false;
            } else if (posX > maxX) {
              setToHandle(maxX);
              return false;
            }
              
            // Update interval bounds indicator
            intervalBounds.css('left', posX);
            intervalBounds.css('width', maxX - posX + handleOffset);
            
            // Update handle label
            fromHandleLabel.html(formatYear(getSelectedRange().from));
          } else {
            // Right handle constraint check
            minX = fromHandle.position().left + 2 * handleOffset;
            maxX = canvas.position().left + canvas.outerWidth();
            
            if (posX < minX) {
              setFromHandle(minX);
              return false;
            } else if (posX > maxX) {
              setToHandle(maxX);
              return false;
            }

            // Update interval bounds indicator
            intervalBounds.css('width', posX - minX + handleOffset);           
                        
            // Update handle label
            toHandleLabel.html(formatYear(getSelectedRange().to));
          }
        },
        
        onDragBounds = function(e) {
          var canvasOffset = canvas.position().left,
              fromX = intervalBounds.position().left - canvasOffset,
              fromYear = xToYear(fromX),
              toX = fromX + intervalBounds.outerWidth(),
              toYear = xToYear(toX);
              
          // Drag handles
          fromHandle.css('left', intervalBounds.position().left - handleOffset);
          toHandle.css('left', intervalBounds.position().left + intervalBounds.outerWidth() - handleOffset);
          
          if (onIntervalChanged) {
            onIntervalChanged({ from: fromYear, to: toYear });
          }
        },
        
        onDragStop = function() {
          var selectedRange;
          
          fromHandleLabel.empty();
          toHandleLabel.empty();
          
          if (onIntervalChanged) {
            selection = getSelectedRange();
            
            if (selection.from == timeRange.from && selection.to == timeRange.to)
              onIntervalChanged(); // Remove time filter altogether
            else
              onIntervalChanged(selection);
          }
        };
        
    /** Privileged methods **/
    
    this.update = function(values, opt_from, opt_to) {
      if (values.length > 0) {                      
        var maxValue = Math.max.apply(Math, jQuery.map(values, function(value) { return value.val; })),
            minYear = values[0].year,
            maxYear = values[values.length - 1].year,
            width = ctx.canvas.width,
            height = ctx.canvas.height,
            xOffset = 0;
        
        ctx.clearRect (0, 0, canvas[0].width, canvas[0].height);
        
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
          
        timeRange.from = minYear;
        timeRange.to = maxYear;
                
        fromLabel.html(formatYear(minYear));
        toLabel.html(formatYear(maxYear));
        
        resetHandles();
      };
    };
    
    // Initialize interval drag handles
    makeXDraggable(fromHandle, onDragHandle, onDragStop);
    makeXDraggable(toHandle, onDragHandle, onDragStop);
    
    // Initialize interval bounds indicator
    makeXDraggable(intervalBounds, onDragBounds);
  };
  
  return TimeHistogram;

});
