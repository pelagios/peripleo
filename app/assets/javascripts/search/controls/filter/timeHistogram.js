define(['search/events', 'common/formatting'], function(Events, Formatting) {
  
  /** Constants **/
  var  BAR_STROKE = '#3182bd',
       BAR_FILL = '#6baed6',
       MIN_UPDATE_DELAY = 800;
       
  var TimeHistogram = function(parent, eventBroker) {
    
    var container = jQuery(
          '<div id="time-histogram">' +
          '  <canvas width="320" height="40"></canvas>' +
          '  <span class="axislabel from"></span>' +
          '  <span class="axislabel zero">0 AD</span>' +
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
        histogramZeroLabel = container.find('.axislabel.zero'),
        histogramToLabel = container.find('.axislabel.to'),
        
        /** Caches the current histogram range  **/
        histogramRange = false,
        
        /** Ignore subsequent updates **/
        ignoreUpdates = false,
        
        /** Caches the selection range **/
        selectionRange = false,
        
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
          if (!selectionRange) {
            var xFrom = fromHandle.position().left + handleWidth,
                yearFrom = xToYear(xFrom),
              
                xTo = toHandle.position().left,
                yearTo = xToYear(xTo);
                
            if ((Math.ceil(xFrom) >= 0) && (Math.floor(xTo) <= canvasWidth + 2))
              selectionRange = { from: yearFrom, to: yearTo };
          }

          return selectionRange;
        },

        onDragHandle = function(e) {
          var maxX, minX,
              posX = jQuery(e.target).position().left;
          
          // Clear cached range   
          selectionRange = false;
          
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
            fromHandleLabel.html(Formatting.formatYear(xToYear(posX + handleWidth)));
              
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
            toHandleLabel.html(Formatting.formatYear(xToYear(posX)));
            
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
            
          if (selection)
            eventBroker.fireEvent(Events.SEARCH_CHANGED, { timespan: selection });
          else
            // Remove time filter altogether
            eventBroker.fireEvent(Events.SEARCH_CHANGED, { timespan: false }); 
        },
        
        onDragBounds = function(e) {
          var offsetX = selectionBounds.position().left,
              width = selectionBounds.outerWidth(),
              
              fromX = offsetX - canvasOffset,
              fromYear = xToYear(fromX),
              toX = fromX + width,
              toYear = xToYear(toX);
             
          // Clear cached range   
          selectionRange = false;
          
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
          if (!ignoreUpdates) {
            var values = response.time_histogram; 
                             
            if (values && values.length > 0) {                      
              var currentSelection = getSelectedRange(),
                  selectionNewFromX, selectionNewToX, // Updated selection bounds
                  maxValue = Math.max.apply(Math, jQuery.map(values, function(value) { return value.val; })),
                  minYear = values[0].year,
                  maxYear = values[values.length - 1].year,
                  height = ctx.canvas.height - 1,
                  xOffset = 5;
                
              histogramRange = { from: minYear, to: maxYear };
            
              // Relabel
              histogramFromLabel.html(Formatting.formatYear(minYear));
              histogramToLabel.html(Formatting.formatYear(maxYear));
              
              if (minYear < 0 && maxYear > 0) {
                histogramZeroLabel.show();
                histogramZeroLabel[0].style.left = (yearToX(0) - 35) + 'px';
              } else { 
                histogramZeroLabel.hide();
              }
            
              // Redraw
              ctx.clearRect(0, 0, canvasWidth, ctx.canvas.height);
              
              // Zero AD marker
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

              // Reset labels & selection      
              histogramRange.from = minYear;
              histogramRange.to = maxYear;
            
              selectionNewFromX = Math.max(2 * handleWidth + 1, yearToX(currentSelection.from));
              selectionNewToX = Math.min(yearToX(currentSelection.to), canvasOffset + canvasWidth + 2);
              if (selectionNewFromX > selectionNewToX)
                selectionNewFromX = selectionNewToX;

              selectionBounds.css('left', selectionNewFromX);
              fromHandle.css('left', selectionNewFromX - handleWidth);
              
              selectionBounds.css('width', selectionNewToX - selectionNewFromX - 1);
              toHandle.css('left', selectionNewToX);
            
              // We don't want to handle to many updates - introduce a wait
              ignoreUpdates = true;
              setTimeout(function() { ignoreUpdates = false; }, MIN_UPDATE_DELAY);
            }
          }
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
