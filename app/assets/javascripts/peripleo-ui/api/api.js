/** A wrapper around the API functions required by the map search UI **/
define(['peripleo-ui/events/events', 'peripleo-ui/api/apiFilterParser'], function(Events, FilterParser) {
  
      /** A throttle for allowing max. one query every QUERY_DELAY_MS milliseconds **/
  var QUERY_DELAY_MS = 100,
  
      /** Number of top places to fetch when there is no query phrase **/
      NUM_TOP_PLACES_WITHOUT_QUERY = 20,
      
      /** Number of top places to fetch when there is a query phrase **/
      NUM_TOP_PLACES_WITH_QUERY = 600,
      
      /** Number of search results to fetch **/
      SEARCH_RESULT_LIMIT = 20,
      
      /** Enum for search states **/
      SearchState = { SEARCH : 1, SUB_SEARCH : 2 };
  
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
          
          bbox: false,
          
          places: false
          
        },
        
        /** Flag indicating whether we're currently in 'serch' or 'subsearch' state **/
        currentSearchState = SearchState.SEARCH,
        
        /** Flag indicating whether time histogram should be included **/
        includeTimeHistogram = false,
        
        /** Indicates whether we're currenly waiting for an API response **/
        busy = false,

        /** Indicating whether the user has already issued a new search/view update request while busy **/
        pendingSearch = false,
        pendingViewUpdate = false,
        
        /** The last search parameter change **/
        lastDiff = false,

        /** Builds the URL query string **/
        buildQueryURL = function(params, searchState) {
          var url = '/peripleo/search?verbose=true&limit=' + SEARCH_RESULT_LIMIT + '&facets=true&top_places=';
          
          if (!params)
            params = searchParams;
            
          if (!searchState)
            searchState = currentSearchState;
                    
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
          
          if (searchState === SearchState.SUB_SEARCH)
            url += '&places=' + jQuery.map(params.places, function(uri) { return encodeURIComponent(uri); }).join(','); 
          else if (params.bbox)
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
          var params = jQuery.extend({}, searchParams), // Params at time of query
              state = currentSearchState; // Search state at time of query
              diff = lastDiff; // Keep most recent diff at time of query

          busy = true;
          
          jQuery.getJSON(buildQueryURL(), function(response) {    
            response.params = params;  
            response.diff = diff;      
            
            if (state === SearchState.SEARCH) {
              eventBroker.fireEvent(Events.API_SEARCH_RESPONSE, response);
              eventBroker.fireEvent(Events.API_VIEW_UPDATE, response);
            } else {
              eventBroker.fireEvent(Events.API_SUB_SEARCH_RESPONSE, response);
              makeViewUpdateRequest(); // In sub-search state, view-updates are different, so we want an extra request
            }
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
          
          // View updates ignore the state, and are always forced to 'search'
          jQuery.getJSON(buildQueryURL(undefined, SearchState.SEARCH), function(response) {
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
        
        /** Changes the search state to 'subsearch' **/
        toStateSubSearch = function(subsearch) {
          currentSearchState = SearchState.SUB_SEARCH;
          searchParams.places = jQuery.map(subsearch.places, function(p) { return p.identifier; });
          if (subsearch.clear_query)
            searchParams.query = false;
          
          search();
        },
        
        /** Changes the search state to 'search' **/
        toStateSearch = function() {
          currentSearchState = SearchState.SEARCH;
          searchParams.places = false;
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
          
          // One-time searches ignore the state, and are always forced to 'sub-search'
          jQuery.getJSON(buildQueryURL(mergedParams, SearchState.SUB_SEARCH), function(response) { 
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
    
    eventBroker.addHandler(Events.ONE_TIME_SEARCH, makeOneTimeSearchRequest);
    
    eventBroker.addHandler(Events.TO_STATE_SUB_SEARCH, toStateSubSearch);
    eventBroker.addHandler(Events.SELECTION, toStateSearch);
    
    // If the filter panel is closed, we don't request the time histogram (it's expensive!)
    eventBroker.addHandler(Events.SHOW_FILTERS, function() {
      includeTimeHistogram = true;
      
      // Filter elements will ignore view updates while in sub-search
      if (currentSearchState === SearchState.SEARCH)
        updateView();
      else
        search();
    });
    
    eventBroker.addHandler(Events.HIDE_FILTERS, function() {
      includeTimeHistogram = false;
    });

  };
  
  return API;
  
});
