/** In charge of updating the URL bar hash segment with current map & search settings **/
define(['peripleo-ui/events/events'], function(Events) {
  
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
        
        updateNow = function() {
          var segment = jQuery.map(segments, function(val, key) {
            return key + '=' + val;
          });

          window.location.hash = segment.join('&');
        },
                
        updateURLField = function() {
          var scheduleUpdate = function() {
                busy = true;
                setTimeout(function() {
                  updateNow();
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
      segments.bbox = 
        bounds.west.toFixed(9) + ',' + bounds.east.toFixed(9) + ',' +
        bounds.south.toFixed(9) + ',' + bounds.north.toFixed(9);
      updateURLField()
    });
    
    eventBroker.addHandler(Events.SEARCH_CHANGED, function(diff) {
      setParam('query', diff);
      setParam('from', diff);
      setParam('to', diff);
      updateURLField();
    });
    
    eventBroker.addHandler(Events.CHANGE_LAYER, function(layer) {
      if (layer === 'awmc')
        delete segments.layer;
      else
        segments.layer = layer;
      updateNow();
    });
    
    eventBroker.addHandler(Events.SELECTION, function(selectedItems) {
      // TODO multi-select?
      var selection = (selectedItems) ? selectedItems[0] : false;
      if (selection) {
        segments.selected = encodeURIComponent(selection.identifier);
      } else {
        delete segments.selected;
      }
      updateNow();
    });
    
    eventBroker.addHandler(Events.SHOW_FILTERS, function() {
      segments.f = 'open';
      updateNow();
    });

    eventBroker.addHandler(Events.HIDE_FILTERS, function() {
      delete segments.f;
      updateNow();
    });
    
    eventBroker.addHandler(Events.START_EXPLORATION, function() {
      segments.ex = 'true';
      updateNow();
    });
    
    eventBroker.addHandler(Events.STOP_EXPLORATION, function() {
      delete segments.ex;
      updateNow();
    });
  };
  
  return URLBar;
    
});
