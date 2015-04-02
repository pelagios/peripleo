define(['search/events'], function(Events) {
  
  /** Constants **/
  var  BAR_STROKE = '#3182bd',
       BAR_FILL = '#6baed6';
       
  var TimeHistogram = function(parent, eventBroker) {
    
    var container = jQuery(
          '<div id="time-histogram">' +
          '  <canvas width="280" height="50"></canvas>' +
          '  <span class="axislabel from"></span>' +
          '  <span class="axislabel to"></span>' +
          
          '  <div class="selection"></div>' +
          
          '  <div class="handle from">&#xe801;' +
          '    <div class="label"></div>' +
          '  </div>' +
          
          '  <div class="handle to">&#xe801;' +
          '    <div class="label"></div>' +
          '  </div>' +
          '</div>'),
     
        /** Canvas element **/
        canvas = container.find('canvas'),
        
        canvasWidth,
        
        canvasOffset,
        
        /** Drawing context - initialize after appending canvas to DOM **/
        ctx, 
        
        /** Selected interval bounds indicator **/
        selectionBounds = container.find('.selection'),
        
        /** Interval handle elements **/
        fromHandle = container.find('.handle.from'),
        fromHandleLabel = fromHandle.find('.label'),
        
        toHandle = container.find('.handle.to'),
        toHandleLabel = toHandle.find('.label'),
        
        /** It's safe to assume that both handles are identical **/
        handleWidth,
        
        /** Labels for earliest/latest year of histogram **/
        histogramFromLabel = container.find('.axislabel.from'),
        histogramToLabel = container.find('.axislabel.to'),
        
        /** Caches the current histogram range  **/
        histogramRange = { from: 0, to: 0 },
        
        /** Helper to format an integer year for screen display **/
        formatYear = function(year) { if (year < 0) return -year + ' BC'; else return year + ' AD'; },
        
        /** Conversion function: x offset to year **/
        xToYear = function(x) {
          var duration = histogramRange.to - histogramRange.from,
              yearsPerPixel = duration / canvasWidth;
              
          return Math.round(histogramRange.from + x * yearsPerPixel);          
        },
        
        /** Conversion function: year to x offset **/
        yearToX = function(year) {
          var duration = histogramRange.to - histogramRange.from,
              pixelsPerYear = canvasWidth / duration;
              
          return Math.round((year - histogramRange.from) * pixelsPerYear);
        },
        
        /** Shorthand for making DOM element draggable along X-axis **/
        makeXDraggable = function(element, onDrag, onStop, opt_containment) {
          element.draggable({ 
            axis: 'x', 
            containment: opt_containment,
            drag: onDrag,
            stop: onStop
          });
        },
        
        /** Shorthand to set the left (from) handle + selection interval **/
        setFromHandle = function(x) {
          fromHandle.css('left', x - 1);
          selectionBounds.css('left', x + handleWidth - 1);
        },
        
        /** Shorthand to set the right (to) handle + selection interval **/
        setToHandle = function(x) {
          toHandle.css('left', x);
          selectionBounds.css('width', x - fromHandle.position().left - handleWidth);
        },
        
        /** Make sure the handles are with the restricted area **/
        constrainHandles = function() {    
          var fromHandleMin = canvasOffset - handleWidth,
              toHandleMax = canvasOffset + canvasWidth + 2;
              
          if (fromHandle.position().left < fromHandleMin)
            setFromHandle(fromHandleMin);
                    
          if (toHandle.position().left > toHandleMax)
            setToHandle(toHandleMax)
        },
      
        /** Returns the currently selected time range **/
        getSelectedRange = function() {
          var xFrom = fromHandle.position().left + handleWidth - canvasOffset,
              yearFrom = xToYear(xFrom),
              
              xTo = toHandle.position().left - canvasOffset,
              yearTo = xToYear(xTo);
              
          return { from: yearFrom, to: yearTo };
        },

        onDragHandle = function(e) {
          var maxX, minX,
              posX = jQuery(e.target).position().left;
              
          if (e.target === fromHandle[0]) {
            // Left handle
            minX = canvasOffset - handleWidth - 1;
            maxX = toHandle.position().left - handleWidth;
            
            if (posX < minX) {
              setFromHandle(minX + 1);
              return false;
            } else if (posX > maxX) {
              setFromHandle(maxX);
              return false;
            }
            
            // Update handle label
            fromHandleLabel.html(formatYear(getSelectedRange().from));
              
            // Update selection bounds
            selectionBounds.css('left', posX + handleWidth);
            selectionBounds.css('width', maxX - posX - 2);
          } else {
            // Right handle constraint check
            minX = fromHandle.position().left + handleWidth;
            maxX = canvasOffset + canvasWidth + 2; 

            if (posX < minX) {
              setToHandle(minX);
              return false;
            } else if (posX > maxX) {
              setToHandle(maxX);
              return false;
            }
            
            // Update handle label
            toHandleLabel.html(formatYear(getSelectedRange().to));
            
            // Update selection bounds
            selectionBounds.css('width', posX - minX - 2);           
          }
        },
        
        onStopHandle = function(e) {
          onDragHandle(e);
          
          var selection = getSelectedRange();
          fromHandleLabel.empty();
          toHandleLabel.empty();
            
          if (selection.from == histogramRange.from && selection.to == histogramRange.to)
            // Remove time filter altogether
            eventBroker.fireEvent(Events.SET_TIME_FILTER); 
          else
            eventBroker.fireEvent(Events.SET_TIME_FILTER, selection);
        },
        
        onDragBounds = function(e) {
          var offsetX = selectionBounds.position().left,
              width = selectionBounds.outerWidth(),
              
              fromX = offsetX - canvasOffset,
              fromYear = xToYear(fromX),
              toX = fromX + width,
              toYear = xToYear(toX);
              
          // Drag handles
          fromHandle.css('left', offsetX - handleWidth);
          toHandle.css('left', offsetX + width);
          
          eventBroker.fireEvent(Events.SET_TIME_FILTER, getSelectedRange());
        },
        
        update = function(values) {
          if (values.length > 0) {                      
            var maxValue = Math.max.apply(Math, jQuery.map(values, function(value) { return value.val; })),
                minYear = values[0].year,
                maxYear = values[values.length - 1].year,
                height = ctx.canvas.height - 1,
                xOffset = 3;
        
            ctx.clearRect (0, 0, canvasWidth, ctx.canvas.height);
        
            jQuery.each(values, function(idx, value) {
              var barHeight = Math.sqrt(value.val / maxValue) * height;   
              ctx.strokeStyle = BAR_STROKE;
              ctx.fillStyle = BAR_FILL;
              ctx.beginPath();
              ctx.rect(xOffset + 0.5, height - barHeight + 0.5, 7, barHeight);
              ctx.fill();
              ctx.stroke();
              xOffset += 11;
            });
          
            histogramRange.from = minYear;
            histogramRange.to = maxYear;
                
            histogramFromLabel.html(formatYear(minYear));
            histogramToLabel.html(formatYear(maxYear));    
            
            constrainHandles();    
          };
        };
    
    parent.prepend(container);
    ctx = canvas[0].getContext('2d');
    
    canvasWidth = canvas.outerWidth(false);
    canvasOffset = (canvas.outerWidth(true) - canvasWidth) / 2;
    handleWidth = fromHandle.outerWidth();
    
    /** We listen for new histograms **/
    eventBroker.addHandler(Events.UPDATED_TIME_HISTOGRAM, update);
    
    makeXDraggable(fromHandle, onDragHandle, onStopHandle);
    makeXDraggable(toHandle, onDragHandle, onStopHandle);
    makeXDraggable(selectionBounds, onDragBounds, onDragBounds, canvas);

    this.update = update;
    
  };
  
  return TimeHistogram;

});
