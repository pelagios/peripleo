/** A wrapper around the API functions required by the map search UI **/
define(['search/events'], function(Events) {
  
  var QUERY_DELAY_MS = 500,
      NUM_TOP_PLACES = 20;
  
  var API = function(eventBroker) {
    var filters = {
          
          query: false,
          
          objectType: false,
          
          dataset: false,
          
          timespan: false,
          
          place: false
          
        },
        
        lastBounds = false,
        
        requestQueue = [],
        
        requestPending = false,

        buildQueryURL = function(bounds, includeTimeHistogram) {
          var url = '/api-v3/search?verbose=true&facets=true&top_places=' + NUM_TOP_PLACES;

          // if (includeTimeHistogram)
          //  url += '&time_histogram=true';
          
          if (filters.query)
            url += '&query=' + filters.query;
            
          if (filters.objectType)
            url += '&type=' + filters.objectType;
            
          if (filters.dataset)
            url += '&dataset=' + filters.dataset;
            
          if (filters.timespan)
            url += '&from=' + filters.timespan.from + '&to=' + filters.timespan.to;
            
          if (filters.place)
            url += '&places=' + encodeURIComponent(filters.place);
          
          url += '&bbox=' +
            bounds.west + ',' + bounds.east + ',' + bounds.south + ',' + bounds.north;
          
          return url;
        },
        
        makeRequest = function() {
          // Do we have a heatmap request anywhere in the queue?
          var heatmapRequests = jQuery.grep(requestQueue, function(req) { return req.heatmap; }),
              timeHistogramRequests = jQuery.grep(requestQueue, function(req) { return req.timeHistogram; });
              bounds = requestQueue.pop().bounds,
              includeTimeHistogram = timeHistogramRequests.length > 0,
              includeHeatmap = heatmapRequests.length > 0;
                
          // Clear the request queue
          requestQueue = [];
            
          // Make the request
          jQuery.getJSON(buildQueryURL(bounds, includeTimeHistogram), function(response) {                                
            eventBroker.fireEvent(Events.API_SEARCH_SUCCESS, response);
              
            // if (includeTimeHistogram)
            //  eventBroker.fireEvent(Events.UPDATED_TIME_HISTOGRAM, response.time_histogram);
          })
          .always(function() {
            requestPending = false;
            
            if (requestQueue.length > 0) // New requests arrived in the meantime
              scheduleSearch()
          });
        },
        
        scheduleSearch = function() { 
          // To prevent excessive requests, we always introduce a 250ms wait
          if (!requestPending) {
            requestPending = true;
            window.setTimeout(makeRequest, QUERY_DELAY_MS);
          }
        };
    
    /** Run a full search on initial load **/
    eventBroker.addHandler(Events.LOAD, function(bounds) {
      lastBounds = bounds;
      requestQueue.push({ bounds: bounds, timeHistogram: true, heatmap: true });
      makeRequest();
    });
    
    eventBroker.addHandler(Events.UI_MAP_CHANGED, function(bounds) {
      lastBounds = bounds;
      requestQueue.push({ bounds: bounds, timeHistogram: true, heatmap: true });
      makeRequest();
    });
    
    eventBroker.addHandler(Events.UI_SEARCH, function(query) {
      filters.query = query;
      requestQueue.push({ bounds: lastBounds, timeHistogram: true, heatmap: false });
      scheduleSearch();
    });
    
    /*
    eventBroker.addHandler(Events.SELECT_PLACE, function(place) {
      if (place)
        filters.place = place.gazetteer_uri;
      else 
        filter.place = false;
      requestQueue.push({ bounds: lastBounds, timeHistogram: true, heatmap: false });
      scheduleSearch();
    });
    */
    
  };
  
  return API;
  
});
