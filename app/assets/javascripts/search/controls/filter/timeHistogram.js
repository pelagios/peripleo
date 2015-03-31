define(['search/events'], function(Events) {
  
  /** Constants **/
  var  BAR_STROKE = '#3182bd',
       BAR_FILL = '#6baed6';
       
  var TimeHistogram = function(parent, eventBroker) {
    
    var container = jQuery(
          '<div id="time-histogram">' +
          '  <span class="label from"></span>' +
          '  <canvas width="180" height="120"></canvas>' +
          '  <span class="label to"></span>' +
          
          '  <div class="handle from">' +
          '    <div class="label"></div>' +
          '  </div>' +
          
          '  <div class="handle to">' +
          '    <div class="label"></div>' +
          '  </div>' +
          '</div>'),
     
        /** Canvas element **/
        canvas = container.find('canvas'),
        
        /** Drawing context - initialize after appending canvas to DOM **/
        ctx, 
        
        /** Selected interval bounds indicator **/
        selectionBounds = container.find('.intervalbounds');
        
        /** Interval handle elements **/
        fromHandle = container.find('.handle.from'),
        fromHandleLabel = fromHandle.find('.label'),
        
        toHandle = container.find('.handle.to'),
        toHandleLabel = toHandle.find('.label'),
        
        /** It's safe to assume that both handles are identical **/
        handleWidth = fromHandle.outerWidth,
        
        /** Labels for earliest/latest year of histogram **/
        histogramFromLabel = container.find('.label.from'),
        histogramToLabel = container.find('.label.to'),
        
        /** Caches the current histogram range  **/
        histogramRange = { from: 0, to: 0 },
        
        /** Helper to format an integer year for screen display **/
        formatYear = function(year) { if (year < 0) return -year + ' BC'; else return year + ' AD'; },
        
        /** Conversion function: x offset to year **/
        xToYear = function(x) {
          var duration = histogramRange.to - histogramRange.from,
              yearsPerPixel = duration / canvas.outerWidth();
              
          return Math.round(histogramRange.from + x * yearsPerPixel);          
        },
        
        /** Conversion function: year to x offset **/
        yearToX = function(year) {
          var duration = histogramRange.to - histogramRange.from,
              pixelsPerYear = canvas.outerWidth() / duration;
              
          return Math.round((year - histogramRange.from) * pixelsPerYear);
        },
        
        /** Shorthand for making DOM element draggable along X-axis **/
        makeXDraggable = function(element, onDrag, onStop) {
          element.draggable({ 
            axis: 'x', 
            drag: onDrag,
            stop: onStop
          });
        },
        
        /** Shorthand to set the left (from) handle + selection interval **/
        setFromHandle = function(x) {
          fromHandle.css('left', x - handleWidth);
          intervalBounds.css('left', x);
        },
        
        /** Shorthand to set the right (to) handle + selection interval **/
        setToHandle = function(x) {
          toHandle.css('left', x);
          intervalBounds.css('width', x - fromHandle.position().left);
        },
      
        /** Returns the currently selected time range **/
        getSelectedRange = function() {
          var xFrom = fromHandle.position().left + handleWidth - canvas.position().left,
              yearFrom = xToYear(xFrom),
              
              xTo = toHandle.position().left - canvas.position().left,
              yearTo = xToYear(xTo);
              
          return { from: yearFrom, to: yearTo };
        },
        
        /** Make sure the handles are inside the canvas area
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
        */
        

        


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
        },
        
        update = function(values) {
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
          
            histogramRange.from = minYear;
            histogramRange.to = maxYear;
                
            histogramFromLabel.html(formatYear(minYear));
            histogramToLabel.html(formatYear(maxYear));        
          };
        };
    
    parent.prepend(container);
    ctx = canvas[0].getContext('2d');
    
    /** We listen for new histograms **/
    eventBroker.addHandler(Events.UPDATED_TIME_HISTOGRAM, update);
    
    // makeXDraggable(fromHandle, onDragHandle, onDragStop);
    // makeXDraggable(toHandle, onDragHandle, onDragStop);
    // makeXDraggable(intervalBounds, onDragBounds);
    
    // Events

    this.update = update;
    
  };
  
  return TimeHistogram;

});
