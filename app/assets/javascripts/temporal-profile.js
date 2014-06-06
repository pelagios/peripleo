window.TemporalProfile = function(divId, data) {
  var div = $('#' + divId),
      width = div.width(),
      height = div.height(),
      canvas = $('<canvas width="' + width + '" height="' + height + '">'),
      ctx = canvas[0].getContext('2d'),
      intervalStart = data.bounds_start,
      intervalEnd = data.bounds_end,
      intervalSize = intervalEnd - intervalStart,
      bucketSize = Math.ceil(intervalSize / width);
      
  div.append(canvas);
  ctx.fillStyle = '#5483bd';
  
  var currentYear = data.bounds_start,
      buckets = [],
      maxValue = 0;
      
  while (currentYear <= intervalEnd) {
    // Aggregate multiple years into buckets, according to histogram screensize
    var bucketValue = 0;
    
    for (var i = 0;  i < bucketSize; i++) {
      var currentValue = data.histogram[currentYear]
      if (currentValue)
        bucketValue += currentValue;
      
      currentYear++;
    }     
    
    if (bucketValue > maxValue)
      maxValue = bucketValue;
    buckets.push(bucketValue);    
  }
  
  $.each(buckets, function(offset, value) {
    var h = value * height * 0.9 / maxValue;
    ctx.fillRect(offset, height - h, 1, h);
  });
}
