define(['search/events', 'common/formatting'], function(Events, Formatting) {
  
  var FilterSettingsPopup = function(eventBroker) {
    var element = jQuery(
          '<div class="clicktrap">' +
          '  <div id="filter-editor">' +
          '   <span class="close icon">&#xf057;</span>' +
          '   <div class="mode-selector">' + 
          '     <span class="btn hide selected">' +
          '       <span class="icon">&#xf00d;</span> Hide selected</span><span class="btn show">' +
          '       <span class="icon">&#xf00c;</span> Show only selected</span>' +
          '   </div>' +
          '   <ul class="chart large"></ul>' +
          '  </div>' +
          '</div>'
        ),
        
        btnModeShow = element.find('.show'),
        btnModeHide = element.find('.hide'),
        btnClose = element.find('.close'),
        
        toggleSwitches,
        
        list = element.find('.chart'),
        
        setModeShow = function() {
          btnModeShow.addClass('selected');
          btnModeHide.removeClass('selected');
          element.find('.selection-toggle').html('&#xf00c;');
        },
        
        setModeHide = function() {
          btnModeShow.removeClass('selected');
          btnModeHide.addClass('selected');
          element.find('.selection-toggle').html('&#xf00d;');
        },
        
        /** Shorthand function for sorting facet values by count **/
        sortFacetValues = function(a,b) { return b.count - a.count },
        
        editFilterSettings = function(facetValues) {
          var dim = facetValues.dimension,
              facets = facetValues.facets,
              maxCount = (facets.length > 0) ? facets.slice().sort(sortFacetValues)[0].count : 0;
              
          list.removeClass();
          list.addClass('chart large ' + dim);
          list.empty();
 
          jQuery.each(facets, function(idx, val) {
            var label = Formatting.formatFacetLabel(val.label),
                tooltip = Formatting.formatNumber(val.count) + ' Results',
                percentage = 100 * val.count / maxCount,
                meter = Formatting.createMeter(label, tooltip, percentage);
                
            meter.prepend('<span class="icon selection-toggle">&#xf00d;</span>');
                
            list.append(meter);
          });

          element.show();  
        };
      
    element.hide();
    jQuery(document.body).append(element);
    
    btnModeShow.click(setModeShow);
    btnModeHide.click(setModeHide);
    btnClose.click(function() { element.hide(); });
    
    eventBroker.addHandler(Events.EDIT_FILTER_SETTINGS, editFilterSettings);
    
  };
  
  return FilterSettingsPopup;
    
});
