define(function() {
  
  return {
    
    /** onLoad(bounds) - initial load event, with map bounds **/
    LOAD : 'load',
    
    /** fn(bounds) - the UI requests an updated heatmap **/
    REQUEST_UPDATED_HEATMAP: 'requestHeatmap',
    
    /** fn(heatmap) - an updated heatmap arrived from the API **/
    UPDATED_HEATMAP: 'updatedHeatmap',

    /** fn(bounds) - the UI requests updated search result and facet counts **/
    REQUEST_UPDATED_COUNTS: 'requestCounts',

    /** fn(values) - updated time histogram arrived from the API **/    
    UPDATED_TIME_HISTOGRAM: 'updatedTimeHistogram',
    
    /** fn(counts) - updated total & facet counts arrived from the API **/
    UPATED_COUNTS: 'updatedCounts',
        
    /** fn(query) - fires when the user issues a text search query **/
    QUERY : 'query',
    
    /** fn(range) - fires when the user sets a time filter **/
    SET_TIME_FILTER : 'setTimeFilter'
    
  };
    
});
