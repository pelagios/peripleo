require(['common/autocomplete', 'common/densityGrid', 'common/timeHistogram'], function(AutoComplete, DensityGrid, TimeHistogram) {
  
  jQuery(document).ready(function() {
    var timeHistogram = new TimeHistogram('time-histogram', function(interval) {
          queryFilters.timespan = interval;
          update();
          refreshHeatmap();
        }),

        autoComplete = new AutoComplete(searchForm, searchInput);
                
        /** Helper to parse the source facet label **/
        parseSourceFacetLabel = function(labelAndId) { 
          var separatorIdx = labelAndId.indexOf('#'),
              label = labelAndId.substring(0, separatorIdx),
              id = labelAndId.substring(separatorIdx);
              
          return { label: label, id: id };
        },
        
        search = function() {
          queryFilters.query = searchInput.val();
          searchInput.blur();
          update();
          refreshHeatmap();
          return false; // preventDefault + stopPropagation
        };        
  });
  
});
