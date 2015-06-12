define(function() {
    
  return {
    
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* General lifecycle events       */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    
    /**
     * Initial page LOAD event. Fired after the page has loaded.
     * 
     * @param initial conditions & search params
     */
    LOAD : 'load',
    
    
    
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* API-related events             */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    
    /** The API returned an initial search result **/
    API_INITIAL_RESPONSE : 'initialLoad',
    
    /**
     * The API returned a search result
     * 
     * @param search result
     */
    API_SEARCH_RESPONSE : 'searchResponse',  
    
    /**
     * The API returned a result for a sub-search
     * 
     * @param search result
     */
    API_SUB_SEARCH_RESPONSE : 'subSearchResponse',
    
    /**
     * The API returned a data to update the map view
     *
     * @param search result
     */
    API_VIEW_UPDATE : 'viewUpdate',    
    
    
    
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* General/global UI events       */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */   
    
    /**
     *  Generic 'selection' event triggered when the users selected a marker or result
     */
    SELECTION : 'selection',
    
    
        
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* Map-related UI events          */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    
    /**
     * The user changed the map viewport.
     * 
     * @param new map bounds
     */
    VIEW_CHANGED : 'viewChanged',
    
    /**
     * The user selected a marker on the map
     * 
     * @param place or array of places
     */
    SELECT_MARKER : 'selectMarker',
    
        
    
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* Search-related UI events       */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    
    /**
     * The user changed any of the search parameters, e.g. by typing & hitting ENTER in the search box
     * or by changing the filter settings
     * 
     * @param change
     */
    SEARCH_CHANGED : 'searchChanged',
    
    /**
     * The users switched to sub-search state (i.e. a search restricted by place URI(s), but not map viewport).
     *  
     * @param subsearch
     */
    TO_STATE_SUB_SEARCH : 'toStateSubSearch',
    
    /**
     * Requests a one-time search from the API. The result will not trigger the global
     * event pool; the search request will always be fired immediately (i.e. not affected
     * by caching or delay policies); and the response will be passed back to a callback
     * function to be provided as parameter.
     * 
     * @param any search parameter that should be different than the current search state
     * plus a callback function
     */   
    ONE_TIME_SEARCH : 'oneTimeSearch',
    
    
    
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* Control-related UI events       */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */    
    
    /** 
     * The controls panel is going through an animated change
     */
    CONTROLS_ANIMATION: 'controlsAnim',
    
    /**
     * The controls panel has finished an animated change
     */
    CONTROLS_ANIMATION_END: 'controlsAnimEnd',
    
    /** 
     * The users has opened the filters panel
     */
    SHOW_FILTERS : 'showFilters',
    
    /**
     * The user has hidden the filters panel
     */
    HIDE_FILTERS : 'hideFilters',
    
    /**
     * The user clicked the 'Set Filter' button
     */
    EDIT_FILTER_SETTINGS : 'editFilterSettings',
    
    /** 
     * Event for showing the result list box. (Can either happen as a
     * user action or as a result of a search response.)
     */
    SHOW_ALL_RESULTS : 'showAllResults',

    /** 
     * Event for hiding the result list box.
     */
    HIDE_ALL_RESULTS : 'hideAllResults',
    
    /** 
     * Event for toggling the visibility of the result list box. (Happens
     * as a user action.)
     */
    TOGGLE_ALL_RESULTS : 'toggleAllResults',
    
    /**
     * The user hovers over a result in the list
     * 
     * @param the search result
     */
    MOUSE_OVER_RESULT : 'mouseOverResult',
    
    /**
     * The user selected a result in the list
     * 
     * @param the result
     */
    SELECT_RESULT : 'selectResult'
    
  };
    
});
