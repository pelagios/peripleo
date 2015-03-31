/** A wrapper around the API functions required by the map search UI **/
define(['search/events'], function(Events) {
  
  var API = function(eventBroker) {
    var filters = {
          
          query: false,
          
          objectType: false,
          
          dataset: false,
          
          timespan: false
          
        },
        
        pendingRequest = false,

        buildQueryURL = function(bounds, includeTimeHistogram, includeHeatmap) {
          var url = '/api-v3/search?facets=true';

          if (includeTimeHistogram)
            url += '&timehistogram=true';
          
          if (includeHeatmap)
            url += '&heatmap=true';
          
          if (filters.query)
            url += '&query=' + filters.query;
            
          if (filters.objectType)
            url += '&type=' + filters.objectType;
            
          if (filters.dataset)
            url += '&dataset=' + filters.dataset;
            
          if (filters.timespan)
            url += '&from=' + filters.timespan.from + '&to=' + filters.timespan.to;
          
          url += '&bbox=' +
            bounds.west + ',' + bounds.east + ',' + bounds.south + ',' + bounds.north;
          
          return url;
        },
        
        doSearch = function(bounds, includeTimeHistogram, includeHeatmap) {
          if (!pendingRequest) {    
            pendingRequest = true;
            
            jQuery.getJSON(buildQueryURL(bounds, includeTimeHistogram, includeHeatmap), function(response) {
              eventBroker.fireEvent(Events.UPATED_COUNTS, response);
              
              if (includeTimeHistogram)
                eventBroker.fireEvent(Events.UPDATED_TIME_HISTOGRAM, response.time_histogram);
              
              if (includeHeatmap)
                eventBroker.fireEvent(Events.UPDATED_HEATMAP, response.heatmap);
            })
            .always(function() {
              pendingRequest = false;
            });
          }
        };
    
    /** Run a full search (plus time histogram and heatmap) on initial load **/
    eventBroker.addHandler(Events.LOAD, function(bounds) {
      doSearch(bounds, true, true);
    });
    
    /** Heatmaps are expensive anyway - so we'll just fetch everything **/
    eventBroker.addHandler(Events.REQUEST_UPDATED_HEATMAP, function(bounds) {
      doSearch(bounds, true, true);
    });
    
    /** Fetch counts **/
    eventBroker.addHandler(Events.REQUEST_UPDATED_COUNTS, function(bounds) {
      doSearch(bounds, true, false);
    });
  };
  
  return API;
  
});
