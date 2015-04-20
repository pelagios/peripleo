define(function() {
  
  return {
    
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* General lifecycle events       */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    
    /**
     * Initial page LOAD event
     * 
     * @param initial map bounds 
     */
    LOAD : 'load',
    
    
    
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* API-related events             */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    /**
     * The API returned a search result
     * 
     * @param search result
     */
    API_SEARCH_SUCCESS : 'searchSuccess',  
    
    
    /**
     * The API returned a search result after a filter change
     * 
     * @param search result
     */
    API_FILTER_SUCCESS : 'filterSuccess',  
    
    
    
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* UI events                      */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    
    /**
     * The user changed the map viewport.
     * 
     * @param new map bounds
     */
    UI_MAP_CHANGED : 'mapChanged',
    
    /**
     * The user triggered a search by typing & hitting ENTER in the search box
     * 
     * @param query phrase
     */
    UI_SEARCH : 'search',
    
    /**
     * The user changed a search filter setting
     * 
     * @param changed filter setting
     */
    UI_CHANGE_FILTER : 'changeFilter',
    
    /** 
     * Event for showing the result list box. (Can either happen as a
     * user action or as a result of a search response.)
     */
    UI_SHOW_ALL_RESULTS : 'showAllResults',

    /** 
     * Event for hiding the result list box.
     */
    UI_HIDE_ALL_RESULTS : 'hideAllResults',
    
    /** 
     * Event for toggling the visibility of the result list box. (Happens
     * as a user action.)
     */
    UI_TOGGLE_ALL_RESULTS : 'toggleAllResults',
    
    /**
     * The user hovers over a result in the list
     * 
     * @param the search result
     */
    UI_MOUSE_OVER_RESULT : 'mouseOverResult',
    
    /**
     * The user selected a place, or places
     * 
     * @param place or array of places
     */
    UI_SELECT_PLACE : 'selectPlace'
    
    
    
    
    
    
    /** TODO REVISE!!! *    
    
    
    /** fn(bounds) - the UI requests an updated heatmap 
    REQUEST_UPDATED_HEATMAP: 'requestHeatmap',
    
    /** fn(heatmap) - an updated heatmap arrived from the API 
    UPDATED_HEATMAP: 'updatedHeatmap',

    /** fn(bounds) - the UI requests updated search result and facet counts 
    REQUEST_UPDATED_COUNTS: 'requestCounts',

    /** fn(values) - updated time histogram arrived from the API 
    UPDATED_TIME_HISTOGRAM: 'updatedTimeHistogram',
    
    /** fn(counts) - updated total & facet counts arrived from the API
    UPATED_COUNTS: 'updatedCounts',
    
    /** fn(results) - new search results arrived from the API 
    UPDATED_SEARCH_RESULTS: 'updatedResults',
    */

    
  };
    
});
