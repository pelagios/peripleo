define(['search/events', 'common/formatting'], function(Events, Formatting) {
  
  /** Constants **/
  var  BAR_STROKE = '#3182bd',
       BAR_FILL = '#6baed6';
       
  var TimeHistogram = function(parent, eventBroker) {
    
    var container = jQuery(
          '<div id="time-histogram">' +
          '  <canvas width="320" height="40"></canvas>' +
          '  <span class="axislabel from"></span>' +
          '  <span class="axislabel to"></span>' +
          
          '  <div class="selection"></div>' +
          
          '  <div class="handle from">' +
          '    <div class="label"></div>' +
          '  </div>' +
      
          '  <div class="handle to">' +
          '    <div class="label"></div>' +
          '  </div>' +
          '</div>'),
     
        /** Canvas element **/
        canvas = container.find('canvas'),
        
        canvasWidth, canvasOffset,
        
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
            minX = handleWidth + 1;
            maxX = toHandle.position().left - handleWidth;
            
            if (posX < minX) {
              fromHandle.css('left', minX);
              return false;
            } else if (posX > maxX) {
              fromHandle.css('left', maxX);
              return false;
            }
            
            // Update handle label
            fromHandleLabel.show();
            fromHandleLabel.html(Formatting.formatYear(getSelectedRange().from));
              
            // Update selection bounds
            selectionBounds.css('left', posX + handleWidth);
            selectionBounds.css('width', maxX - posX - 1);
          } else {
            // Right handle constraint check
            minX = fromHandle.position().left + handleWidth + 1;
            maxX = canvasOffset + canvasWidth + 2; 
            
            if (posX < minX) {
              toHandle.css('left', minX + 1);
              return false;
            } else if (posX > maxX) {
              toHandle.css('left', maxX);
              return false;
            }
            
            // Update handle label
            toHandleLabel.show();
            toHandleLabel.html(Formatting.formatYear(getSelectedRange().to));
            
            // Update selection bounds
            selectionBounds.css('width', posX - minX);        
          }
        },
        
        onStopHandle = function(e) {
          onDragHandle(e);
          
          var selection = getSelectedRange();
          fromHandleLabel.empty();
          fromHandleLabel.hide();
          toHandleLabel.empty();
          toHandleLabel.hide();
            
          if (selection.from <= histogramRange.from && selection.to >= histogramRange.to) {
            // Remove time filter altogether
            eventBroker.fireEvent(Events.SEARCH_CHANGED, { timespan: false }); 
          } else {
            eventBroker.fireEvent(Events.SEARCH_CHANGED, { timespan: selection });
          }
        },
        
        onDragBounds = function(e) {
          var offsetX = selectionBounds.position().left,
              width = selectionBounds.outerWidth(),
              
              fromX = offsetX - canvasOffset,
              fromYear = xToYear(fromX),
              toX = fromX + width,
              toYear = xToYear(toX);
              
          fromHandleLabel.html(Formatting.formatYear(fromYear));
          fromHandleLabel.show();
          fromHandle.css('left', offsetX - handleWidth);
          
          toHandleLabel.html(Formatting.formatYear(toYear));
          toHandleLabel.show();
          toHandle.css('left', offsetX + width);

          eventBroker.fireEvent(Events.SEARCH_CHANGED, { timespan: getSelectedRange() });
        },
        
        onStopBounds = function(e) {
          onDragBounds(e);
          
          fromHandleLabel.empty();
          fromHandleLabel.hide();
          
          toHandleLabel.empty();
          toHandleLabel.hide();
        },
        
        update = function(response) {
          var values = response.time_histogram;                    
          if (values && values.length > 0) {                      
            var maxValue = Math.max.apply(Math, jQuery.map(values, function(value) { return value.val; })),
                minYear = values[0].year,
                maxYear = values[values.length - 1].year,
                height = ctx.canvas.height - 1,
                xOffset = 5;
        
            ctx.clearRect (0, 0, canvasWidth, ctx.canvas.height);
        
            jQuery.each(values, function(idx, value) {
              var barHeight = Math.round(Math.sqrt(value.val / maxValue) * height);   
              ctx.strokeStyle = BAR_STROKE;
              ctx.fillStyle = BAR_FILL;
              ctx.beginPath();
              ctx.rect(xOffset + 0.5, height - barHeight + 0.5, 5, barHeight);
              ctx.fill();
              ctx.stroke();
              xOffset += 9;
            });
          
            histogramRange.from = minYear;
            histogramRange.to = maxYear;
                
            histogramFromLabel.html(Formatting.formatYear(minYear));
            histogramToLabel.html(Formatting.formatYear(maxYear));      
          };
        };
    
    fromHandleLabel.hide();
    toHandleLabel.hide();
    parent.append(container);
    
    ctx = canvas[0].getContext('2d');
    canvasWidth = canvas.outerWidth(false);
    canvasOffset = (canvas.outerWidth(true) - canvasWidth) / 2;
    handleWidth = fromHandle.outerWidth();
    
    makeXDraggable(fromHandle, onDragHandle, onStopHandle);
    makeXDraggable(toHandle, onDragHandle, onStopHandle);
    makeXDraggable(selectionBounds, onDragBounds, onStopBounds, canvas);

    eventBroker.addHandler(Events.API_VIEW_UPDATE, update);
    
  };
  
  return TimeHistogram;

});
