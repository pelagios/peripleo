/** In charge of updating the URL bar hash segment with current map & search settings **/
define(['search/events'], function(Events) {
  
  // Make sure we don't update the URL bar too frequently as it
  // introduces noticable delays
  var SLEEP_DURATION = 1000;
  
  var URLBar = function(eventBroker) {
    
    var segments = {},
          
        busy = false,
          
        updatePending = false,
        
        /** Updates a particular segment field with the value from the diff, if any **/
        setParam = function(name, diff) {
          if (diff.hasOwnProperty(name)) {
            if (diff[name])
              segments[name] = diff[name];
            else // diff[name] = false -> remove this segment field
              delete segments[name];
          }          
        },
                
        updateURLField = function() {
          var scheduleUpdate = function() {
                busy = true;
                setTimeout(function() {
                  var segment = jQuery.map(segments, function(val, key) {
                    return key + '=' + val;
                  });

                  window.location.hash = segment.join('&');
                  busy = false;
                  if (updatePending) {
                    updatePending = false;
                    scheduleUpdate();
                  }
                }, SLEEP_DURATION);
              };
          
          if (busy)
            updatePending = true;
          else
            scheduleUpdate();
        };
    
    eventBroker.addHandler(Events.VIEW_CHANGED, function(bounds) {
      segments.bbox = bounds.west + ',' + bounds.east + ',' + bounds.south + ',' + bounds.north;
      updateURLField()
    });
    
    eventBroker.addHandler(Events.SEARCH_CHANGED, function(diff) {
      setParam('query', diff);
      setParam('from', diff);
      setParam('to', diff);
      
      updateURLField();
    });
    
  };
  
  return URLBar;
    
});
