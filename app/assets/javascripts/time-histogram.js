window.TimeHistogram = (function() {
  
  var PADDING = 10,
      BAR_SPACING = 2,
      BASELINE_COLOR = '#ccc';
      BAR_COLOR = '#99ccff';
  
  var TimeHistogram = function(div, data, steps) {
    var minYear = 9007199254740992,
        maxYear = -9007199254740992, 
        
        bars,
        maxBarValue = 0,
        
        el = jQuery(div),
        canvas,
        
        init = function() {          
          var year, value;
          for (key in data) {
            year = parseInt(key);
            
            if (year < minYear)
              minYear = year;
              
            if (year > maxYear)
              maxYear = year;
          }
        },
        
        toLabel = function(year) {
          if (year < 0) {
            return Math.abs(year) + 'BC';
          } else {
            return year + 'AD';
          }
        },
        
        getSum = function(from, to) {
          var i, value, sum = 0;
          for (i = from; i <= to; i++) {
            value = data[i];
            if (value)
              sum += value;
          }
          return sum
        },
        
        computeBars = function(from, to, steps) {
          var stepWidth = (steps) ? (to - from) / steps : (to - from) / 25,
              sum, year = from, _bars = [];
              
          while (year < to) {
            sum = getSum(Math.floor(year), Math.floor(year + stepWidth))
            if (sum > maxBarValue)
              maxBarValue = sum;
              
            _bars.push(sum);
            year += stepWidth;
          }
          
          return _bars;
        },
        
        draw = function(canvas) {
          var idx, barHeight, xOffset, yOffset,
              ctx = canvas[0].getContext('2d');
              width = canvas.width(),
              height = canvas.height(),
              maxHeight = height / 2 - PADDING,
              yIncrement = maxHeight / maxBarValue,
              barWidth = ((width - 2 * PADDING) / bars.length) - BAR_SPACING;
          
          // Bars
          xOffset = PADDING + BAR_SPACING / 2;
          for (idx in bars) {
            barHeight = bars[idx] * yIncrement;
            yOffset = height / 2 - barHeight;
            
            ctx.fillStyle = BAR_COLOR;
            ctx.beginPath();
            ctx.rect(xOffset, yOffset, barWidth, barHeight * 2);
            ctx.fill();
            
            xOffset += barWidth + BAR_SPACING;
          }
          
          // Baseline
          ctx.strokeStyle = BASELINE_COLOR;
          ctx.lineWidth = 1;
          ctx.beginPath();
          ctx.moveTo(PADDING, Math.round(height / 2) - 0.5);
          ctx.lineTo(width - PADDING, Math.round(height / 2) - 0.5);
          ctx.stroke();
        };
    
    init();
    
    if (minYear < 9007199254740992 && maxYear > -9007199254740992) {
      bars = computeBars(minYear, maxYear, steps);
    
      canvas = jQuery('<canvas>')
        .attr('width', el.width())
        .attr('height', el.height());
      
        el.append(canvas);
        draw(canvas);

      this.minYear = toLabel(minYear);
      this.maxYear = toLabel(maxYear);
    }
  };
  
  return TimeHistogram;
  
})();
