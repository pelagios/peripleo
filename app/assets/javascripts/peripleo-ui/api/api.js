/** A wrapper around the API functions required by the map search UI **/
define(['peripleo-ui/events/events', 'peripleo-ui/api/apiFilterParser'], function(Events, FilterParser) {
  
      /** A throttle for allowing max. one query every QUERY_DELAY_MS milliseconds **/
  var QUERY_DELAY_MS = 100,
  
      /** Number of top places to fetch when there is no query phrase **/
      NUM_TOP_PLACES_WITHOUT_QUERY = 20,
      
      /** Number of top places to fetch when there is a query phrase **/
      NUM_TOP_PLACES_WITH_QUERY = 600,
      
      /** Number of search results to fetch **/
      SEARCH_RESULT_LIMIT = 20;
  
  var API = function(eventBroker) {
  
        /** Current search parameter state **/
    var searchParams = {
          
          query: false,
          
          object_types: false,
          
          exclude_object_types: false,
          
          datasets: false,
          
          exclude_datasets: false,
          
          gazetteers: false,
          
          exclude_gazetteers: false,
          
          from: false,
          
          to: false,
          
          place: false,
          
          bbox: false
          
        },
        
        /** Flag indicating whether time histogram should be included **/
        includeTimeHistogram = false,
        
        /** Indicates whether we're currenly waiting for an API response **/
        busy = false,

        /** Indicating whether the user has already issued a new search/view update request while busy **/
        pendingSearch = false,
        pendingViewUpdate = false,
        
        /** The last search parameter change **/
        lastDiff = false,

        /** Builds the URL query string from the current search params **/
        buildQueryURL = function(params) {
          if (!params)
            params = searchParams;
            
          var url = '/api-v3/search?verbose=true&limit=' + SEARCH_RESULT_LIMIT + 
                    '&facets=true&top_places=';
                    
          if (params.query)
            url += NUM_TOP_PLACES_WITH_QUERY;
          else 
            url += NUM_TOP_PLACES_WITHOUT_QUERY;
                    
          if (includeTimeHistogram) 
            url += '&time_histogram=true';
          
          if (params.query)
            url += '&query=' + params.query;
            
          if (params.object_types)
            url += '&types=' + params.object_types;
            
          if (params.exclude_object_types)
            url += '&exclude_types=' + params.exclude_object_types;
            
          if (params.datasets)
            url += '&datasets=' + params.datasets;
            
          if (params.exclude_datasets)
            url += '&exclude_datasets=' + params.exclude_datasets;
            
          if (params.gazetteers)
            url += '&gazetteers=' + params.gazetteers;
            
          if (params.exclude_gazetteers)
            url += '&exclude_gazetteers=' + params.exclude_gazetteers;
            
          if (params.from)
            url += '&from=' + params.from;
            
          if (params.to)
            url += '&to=' + params.to;
            
          if (params.place)
            url += '&places=' + encodeURIComponent(params.place);
          
          if (params.bbox)
            url += '&bbox=' +
              params.bbox.west + ',' + params.bbox.east + ',' + 
              params.bbox.south + ',' + params.bbox.north;
          
          return url;
        },
        
        /** Waits for QUERY_DELAY_MS and handles the pending request, if any **/
        handlePending = function() {
          setTimeout(function() {
            if (pendingSearch)
              makeSearchRequest();
            else if (pendingViewUpdate) // Note: search always include view updates, too
              makeViewUpdateRequest();
            else
              busy = false;
            
            pendingSearch = false;
            pendingViewUpdate = false;
          }, QUERY_DELAY_MS);
        },
        
        /** Fires an initial load request **/
        initialLoad = function() {
          busy = true;
          
          jQuery.getJSON(buildQueryURL(), function(response) {
            eventBroker.fireEvent(Events.API_INITIAL_RESPONSE, response);
          }).always(handlePending);          
        },
        
        /** Fires a search request against the API **/
        makeSearchRequest = function() {
          var params = jQuery.extend({}, searchParams), // Clone params at time of query
              diff = lastDiff; // Keep most recent diff at time of query
              
          busy = true;
          
          jQuery.getJSON(buildQueryURL(), function(response) {    
            response.params = params;  
            response.diff = diff;      
            eventBroker.fireEvent(Events.API_SEARCH_RESPONSE, response);
            eventBroker.fireEvent(Events.API_VIEW_UPDATE, response);
          }).always(handlePending);
        },
        
        /** Helper: either fires a search request, or schedules for later if busy **/
        search = function() {
          if (busy)
            pendingSearch = true;
          else 
            makeSearchRequest();
        },
        
        /** Fires a search request against the API to accomodate a view update **/
        makeViewUpdateRequest = function() {     
          busy = true;
          
          jQuery.getJSON(buildQueryURL(), function(response) {
            eventBroker.fireEvent(Events.API_VIEW_UPDATE, response);
          }).always(handlePending);
        },
        
        /** Helper: either fires a view update request, or schedules for later if busy **/
        updateView = function() {
          if (busy)
            pendingViewUpdate = true;
          else
            makeViewUpdateRequest();
        },
        
        /**
         * Fires a sub-search request. A sub-search uses the current global search parameter
         * settings, plus a set of changes. The changes are, however, not remembered beyond
         * the request, nor do they change the current global parameter values.
         * 
         * Unlike normal searches or view-updates, sub-searches are performed immediately.
         * I.e. they are not affected by the 'busy' state, caching or delay policies.
         * 
         * @param diff the changes to the current global search parameters
         */
        makeSubSearchRequest = function(places) {          
          /*
          var mergedParams = jQuery.extend({}, searchParams); // Clone current query state
          jQuery.extend(mergedParams, FilterParser.parseFacetFilter(diff, searchParams)); // Merge current state with diff
          jQuery.getJSON(buildQueryURL(mergedParams), function(response) { 
            response.params = mergedParams;
            eventBroker.fireEvent(Events.API_SUB_SEARCH_RESPONE, response);
          });          
          */
        },
        
        /**
         * Fires a one-time search request. The one-time search uses the current global
         * search parameter settings, plus a set of changes. The request is fired to the API
         * immediately.
         * 
         * The one-time search is similar to the sub-search. However, the result is not
         * communicated via the global event pool. Instead, the response is ONLY passed back
         * to a callback function provided in the parameters.
         * 
         * @param the changes to the current global search parameters, and the callback function
         */        
        makeOneTimeSearchRequest = function(params) {
          var mergedParams = jQuery.extend({}, searchParams); // Clone current query state
          jQuery.extend(mergedParams, FilterParser.parseFacetFilter(params, searchParams)); // Merge current state with params
          jQuery.getJSON(buildQueryURL(mergedParams), function(response) { 
            response.params = mergedParams;
            delete response.params.callback; // Clean up the params object, i.e. remove the callback fn reference
            params.callback(response);
          });
        };

    /** Run an initial view update on load **/
    eventBroker.addHandler(Events.LOAD, function(initialSettings) {
      jQuery.extend(searchParams, initialSettings); // Incorporate inital settings      
      initialLoad();
    });
    
    eventBroker.addHandler(Events.SEARCH_CHANGED, function(diff) {     
      var diffNormalized = FilterParser.parseFacetFilter(diff, searchParams);
       
      jQuery.extend(searchParams, diffNormalized); // Update search params
      lastDiff = diffNormalized; // Store as last diff
    
      // SPECIAL: if the user added a query phrase, ignore geo-bounds
      if (diff.query)
        searchParams.bbox = false;
      
      search();
    });
    
    eventBroker.addHandler(Events.VIEW_CHANGED, function(bounds) {      
      searchParams.bbox = bounds;
      updateView();
    });
    
    eventBroker.addHandler(Events.SUB_SEARCH, makeSubSearchRequest);
    eventBroker.addHandler(Events.ONE_TIME_SEARCH, makeOneTimeSearchRequest);
    
    // Just make sure we clear place filters when places get de-selected
    eventBroker.addHandler(Events.SELECTION, function(obj) {
      searchParams.place = false;
    });
    
    // If the filter panel is closed, we don't request the time histogram (it's expensive!)
    eventBroker.addHandler(Events.SHOW_FILTERS, function() {
      includeTimeHistogram = true;
      updateView();
    });
    
    eventBroker.addHandler(Events.HIDE_FILTERS, function() {
      includeTimeHistogram = false;
    });

  };
  
  return API;
  
});
