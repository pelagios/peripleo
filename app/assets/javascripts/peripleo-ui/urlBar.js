/** In charge of updating the URL bar hash segment with current map & search settings **/
define(['peripleo-ui/events/events'], function(Events) {
  
  // Make sure we don't update the URL bar too frequently as it
  // introduces noticable delays
  var SLEEP_DURATION = 1000;
  
  var URLBar = function(eventBroker) {
    
    var segments = {},
          
        busy = false,
          
        updatePending = false,
        
        /** Helper function to parse a bbox string **/
        parseAt = function(atStr) {
          var values = atStr.split(',');
          return { lat: parseFloat(values[0]), lng: parseFloat(values[1]), zoom: parseFloat(values[2]) }; 
        },
        
        parseURLHash = function(hash) {
          var keysVals = (hash.indexOf('#') === 0) ? hash.substring(1).split('&') : false,
              at;
              
          if (keysVals) {
            jQuery.each(keysVals, function(idx, keyVal) {
              var asArray = keyVal.split('='),
                  key = asArray[0],
                  value = asArray[1];
                       
              if (key === 'at') // Parse bbox
                at = parseAt(value);
              
              segments[key] = value;
            });
            
            // Number parsing for timespan
            if (segments.from)
              segments.from = parseInt(segments.from);

            if (segments.to)
              segments.to = parseInt(segments.to);
              
            var settings = jQuery.extend({}, segments);
            if (at)
              settings.at = at;
            return settings;
          } else {
            return {};
          }
        },
                
        /** Updates the URL field - NOW! **/
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
      var lat = (bounds.south + bounds.north) / 2,
          lon = (bounds.east + bounds.west) / 2; 
      
      segments.at = lat + ',' + lon + ',' + bounds.zoom;
      updateURLField()
    });
    
    eventBroker.addHandler(Events.SEARCH_CHANGED, function(diff) {
      for (param in diff) {     
        if (diff[param])
          segments[param] = diff[param];
        else
          delete segments[param];
      }
      updateNow();
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
      if (selection)
        segments.places = encodeURIComponent(selection.identifier);
      else
        delete segments.places;
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
    
    this.parseURLHash = parseURLHash;
  };
  
  return URLBar;
    
});
