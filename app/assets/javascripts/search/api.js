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
        
        requestQueue = [],
        
        requestPending = false,

        buildQueryURL = function(bounds, includeTimeHistogram) {
          var url = '/api-v3/search?facets=true&top_places=' + NUM_TOP_PLACES;

          if (includeTimeHistogram)
            url += '&time_histogram=true';
          
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
            eventBroker.fireEvent(Events.UPATED_COUNTS, response);
            eventBroker.fireEvent(Events.UPDATED_SEARCH_RESULTS, response.items);
              
            if (includeTimeHistogram)
              eventBroker.fireEvent(Events.UPDATED_TIME_HISTOGRAM, response.time_histogram);
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
    
    /** Run a full search (plus time histogram and heatmap) on initial load **/
    eventBroker.addHandler(Events.LOAD, function(bounds) {
      requestQueue.push({ bounds: bounds, timeHistogram: true, heatmap: true });
      makeRequest();
    });
    
    /** Heatmaps are expensive anyway - so we'll just fetch everything **/
    eventBroker.addHandler(Events.REQUEST_UPDATED_HEATMAP, function(bounds) {
      requestQueue.push({ bounds: bounds, timeHistogram: true, heatmap: true });
      scheduleSearch();
    });
    
    /** Fetch counts **/
    eventBroker.addHandler(Events.REQUEST_UPDATED_COUNTS, function(bounds) {
      requestQueue.push({ bounds: bounds, timeHistogram: true, heatmap: true });
      scheduleSearch();
    });
    
    /** User reset the time filter - queue new search request **/
    eventBroker.addHandler(Events.SET_TIME_FILTER, function(timespan) {
      filters.timespan = timespan;
      requestQueue.push({ bounds: bounds, timeHistogram: false, heatmap: true });
      scheduleSearch();
    }),
    
    eventBroker.addHandler(Events.SELECT_PLACE, function(place) {
      if (place)
        filters.place = place.gazetteer_uri;
      else 
        filter.place = false;
      requestQueue.push({ bounds: bounds, timeHistogram: true, heatmap: false });
      scheduleSearch();
    });
    
  };
  
  return API;
  
});
