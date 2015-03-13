define(function() {

  var TimeHistogram = function(divId, data) {
    var div = $('#' + divId),
      
        /** Drawing canvas **/
        width = div.width(),
        height = div.height(),
        canvas = $('<canvas width="' + width + '" height="' + height + '">'),
        ctx = canvas[0].getContext('2d'),
        
        /** Time profile properties **/
        intervalStart = data.bounds_start,
        intervalEnd = data.bounds_end,
        intervalSize = intervalEnd - intervalStart,
        bucketSize = Math.ceil(intervalSize / width),

        buckets = [],
        maxValue = 0,
        
        /** Running variables **/
        currentYear = data.bounds_start,
        bucketValue, currentValue;
      
    // Compute bucket sizes
    while (currentYear <= intervalEnd) {
      // Aggregate multiple years into buckets, according to histogram screensize
      bucketValue = 0;
    
      for (var i = 0;  i < bucketSize; i++) {
        currentValue = data.histogram[currentYear];
        if (currentValue)
          bucketValue += currentValue;
      
        currentYear++;
      }     
    
      if (bucketValue > maxValue)
        maxValue = bucketValue;
      buckets.push(bucketValue);    
    }
  
    // Render graphic
    div.append(canvas);
    ctx.fillStyle = '#5483bd';
    
    $.each(buckets, function(offset, value) {
      var h = value * height * 0.9 / maxValue;
      ctx.fillRect(offset, height - h, 1, h);
    });
  }
  
  return TimeHistogram;

});
