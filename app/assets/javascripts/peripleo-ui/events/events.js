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
    
    /**
     * The API returned the next search result page
     * 
     * @param search result
     */
    API_NEXT_PAGE  : 'nextPage',
    
    
    
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
     * The users switched back to normal search state.
     */    
    TO_STATE_SEARCH : 'toStateSearch',
    
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
    /* Search-panel-related UI events */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */    

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
     * Event for showing all results in the list box.
     */
    SHOW_ALL_RESULTS : 'showAllResults',
    
    /** 
     * Event for showing subsearch results in the list box.
     */        
    SHOW_SUBSEARCH_RESULTS : 'showSubsearchResults',

    /** 
     * Event for hiding the result list box.
     */
    HIDE_RESULTS : 'hideAllResults',
    
    /**
     * The user hovers over a result in the list.
     * 
     * @param the search result
     */
    MOUSE_OVER_RESULT : 'mouseOverResult',
    
    /**
     * The user selected a result in the list.
     * 
     * @param the result
     */
    SELECT_RESULT : 'selectResult',
    
    /**
     * The user scrolled to the bottom of the results list - load next batch
     * 
     * @param offset
     */
    LOAD_NEXT_PAGE : 'loadNextPage',
    
    
    
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* Toolbar-related UI events      */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */   

    /**
     * The user clicked the map settings button
     */
    EDIT_MAP_SETTINGS : 'editMapSettings',
    
    /**
     * The user clicked the 'zoom in' (plus) button.
     */
    ZOOM_IN : 'zoomIn',

    /**
     * The user clicked the 'zoom out' (minus) button.
     */    
    ZOOM_OUT : 'zoomOut',
    
    
    
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* Settings-related UI events     */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */   
        
    CHANGE_LAYER : 'changeLayer'
        
  };
    
});
