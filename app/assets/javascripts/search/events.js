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
    API_SEARCH_RESPONSE : 'searchResponse',  
    
    
    API_VIEW_UPDATE : 'viewUpdate',    
    
    
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* UI events                      */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    
    /**
     * The user changed the map viewport.
     * 
     * @param new map bounds
     */
    VIEW_CHANGED : 'viewChanged',
    
    /**
     * The user changed the search parameters, e.g. by typing & hitting ENTER in the search box
     * or by changing the filter settings
     * 
     * @param change
     */
    SEARCH_CHANGED : 'searchChanged',
    
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
     * The user selected a marker on the map
     * 
     * @param place or array of places
     */
    SELECT_MARKER : 'selectMarker',
    
    /**
     * The user selected a result in the list
     * 
     * @param the result
     */
    SELECT_RESULT : 'selectResult',
    
    /**
     *  Generic 'selection' event triggered when the users selected a marker or result
     */
    SELECTION : 'selection'
    
  };
    
});
