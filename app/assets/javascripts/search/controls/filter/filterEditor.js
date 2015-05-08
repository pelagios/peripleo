define(['search/events', 'common/formatting'], function(Events, Formatting) {
  
  var FilterSettingsPopup = function(eventBroker) {
    var element = jQuery(
          '<div class="clicktrap">' +
          '  <div id="filter-editor">' +
          '   <span class="close icon">&#xf057;</span>' +
          '   <ul class="chart"></ul>' +
          '  </div>' +
          '</div>'
        ),
        
        btnClose = element.find('.close'),
        
        list = element.find('.chart'),
        
        /** Shorthand function for sorting facet values by count **/
        sortFacetValues = function(a,b) { return b.count - a.count },
        
        editFilterSettings = function(facetValues) {
          var dim = facetValues.dimension,
              facets = facetValues.facets,
              maxCount = (facets.length > 0) ? facets.slice().sort(sortFacetValues)[0].count : 0;
              
          list.removeClass();
          list.addClass('chart ' + dim);
          list.empty();
 
          jQuery.each(facets, function(idx, val) {
            var label = Formatting.formatFacetLabel(val.label),
                tooltip = Formatting.formatNumber(val.count) + ' Results',
                percentage = 100 * val.count / maxCount;
                
            list.append(Formatting.createMeter(label, tooltip, percentage));
          });
          
          element.show();  
        };
      
    element.hide();
    jQuery(document.body).append(element);
    
    btnClose.click(function() { element.hide(); });
    
    eventBroker.addHandler(Events.EDIT_FILTER_SETTINGS, editFilterSettings);
    
  };
  
  return FilterSettingsPopup;
    
});
