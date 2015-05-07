/** In charge of updating the URL bar hash segment with current map & search settings **/
define(['search/events'], function(Events) {
  
  // Make sure we don't update the URL bar too frequently as it
  // introduces noticable delays
  var SLEEP_DURATION = 1000;
  
  var URLBar = function(eventBroker) {
    
    var segments = {},
          
        busy = false,
          
        updatePending = false,
                
        updateURLFragment = function() {
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
      updateURLFragment()
    });
    
    eventBroker.addHandler(Events.SEARCH_CHANGED, function(change) {
      if (change.hasOwnProperty('query')) {
        if (change.query) {
          segments.query = change.query;
        } else {
          delete segments.query;
        }
      }
      
      if (change.hasOwnProperty('timespan')) {
        if (change.timespan) {
          segments.from = change.timespan.from;
          segments.to = change.timespan.to;
        } else {
          delete segments.from;
          delete segments.to;
        }
      }
      
      updateURLFragment();
    });
    
  };
  
  return URLBar;
    
});
