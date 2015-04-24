/** A wrapper around the API functions required by the map search UI **/
define(['search/events'], function(Events) {
  
  var QUERY_DELAY_MS = 100,
  
      NUM_TOP_PLACES = 10,
      
      ITEM_LIMIT = 100;
  
  var API = function(eventBroker) {
  
        /** Current search parameters **/
    var searchParams = {
          
          query: false,
          
          objectType: false,
          
          dataset: false,
          
          timespan: false,
          
          place: false
          
        },
        
        /** The current map bounds **/
        currentMapBounds = false,
        
        /** Indicates whether we're currenly waiting for an API response **/
        busy = false,

        /** Indicating whether the user has already issued a new search/view update request while busy **/
        pendingSearch = false,
        pendingViewUpdate = false,

        /** Builds the URL query string from the current search params **/
        buildQueryURL = function(params) {
          if (!params)
            params = searchParams;
            
          var url = '/api-v3/search?verbose=true&limit=' + ITEM_LIMIT + 
                    '&facets=true&top_places=' + NUM_TOP_PLACES;
          
          if (params.query)
            url += '&query=' + params.query;
            
          if (params.objectType)
            url += '&type=' + params.objectType;
            
          if (params.dataset)
            url += '&dataset=' + params.dataset;
            
          if (params.timespan)
            url += '&from=' + params.timespan.from + '&to=' + params.timespan.to;
            
          if (params.place)
            url += '&places=' + encodeURIComponent(params.place);
          
          // Note: if there's a user queries, we don't want the bounding box limit
          if (!params.query)
            url += '&bbox=' +
              currentMapBounds.west + ',' + currentMapBounds.east + ',' + 
              currentMapBounds.south + ',' + currentMapBounds.north;
          
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
        
        makeSearchRequest = function() {
          var params = jQuery.extend({}, searchParams); // Clone params at time of query
          busy = true;
          
          jQuery.getJSON(buildQueryURL(), function(response) {    
            response.params = params;        
            eventBroker.fireEvent(Events.API_SEARCH_RESPONSE, response);
            eventBroker.fireEvent(Events.API_VIEW_UPDATE, response);
          }).always(handlePending);
        },
        
        search = function() {
          if (busy)
            pendingSearch = true;
          else 
            makeSearchRequest();
        },
        
        makeViewUpdateRequest = function() {     
          busy = true;
          
          jQuery.getJSON(buildQueryURL(), function(response) {
            eventBroker.fireEvent(Events.API_VIEW_UPDATE, response);
          }).always(handlePending);
        },
        
        updateView = function() {
          if (busy)
            pendingViewUpdate = true;
          else
            makeViewUpdateRequest();
        };

    /** Run an initial view update on load **/
    eventBroker.addHandler(Events.LOAD, function(bounds) {
      currentMapBounds = bounds;
      updateView();
    });
    
    eventBroker.addHandler(Events.SEARCH_CHANGED, function(change) {      
      jQuery.extend(searchParams, change); // Merge changes
      search();
    });
    
    eventBroker.addHandler(Events.VIEW_CHANGED, function(bounds) {      
      currentMapBounds = bounds;
      updateView();
    });
    
    eventBroker.addHandler(Events.SELECTION, function(obj) {
      // Just make sure we clear place filters when places get de-selected
      searchParams.place = false;
    });
    
    eventBroker.addHandler(Events.API_DO_ONETIME_SEARCH, function(params) {
      var mergedParams = jQuery.extend({}, searchParams); // Clone current query state
      jQuery.extend(mergedParams, params); // Merge current state with params
      jQuery.getJSON(buildQueryURL(mergedParams), function(response) { 
        response.params = mergedParams;
        delete response.params.callback; // Clean up the params object
        params.callback(response);
      });
    });

  };
  
  return API;
  
});
