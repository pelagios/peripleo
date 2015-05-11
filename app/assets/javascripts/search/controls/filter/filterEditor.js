define(['search/events', 'common/formatting'], function(Events, Formatting) {
  
  var FADE_DURATION = 100;
  
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
        
        selected = [],
        
        selectionHides = true, // Selection mode - true for 'hide selected'
        
        setModeShow = function() {
          selectionHides = false;
          btnModeShow.addClass('selected');
          btnModeHide.removeClass('selected');
          element.find('.selection-toggle').html('&#xf00c;');
          redraw();
        },
        
        setModeHide = function() {
          selectionHides = true;
          btnModeShow.removeClass('selected');
          btnModeHide.addClass('selected');
          element.find('.selection-toggle').html('&#xf00d;');
          redraw();
        },
        
        select = function(element, value) {
          var indexOfVal = selected.indexOf(value.label),
              targetOpacity;
          
          if (element.hasClass('selected')) {
            targetOpacity = (selectionHides) ? 1 : 0.3;
            element.fadeTo(FADE_DURATION, targetOpacity);
            element.removeClass('selected');
            selected.splice(indexOfVal, 1);
          } else {
            targetOpacity = (selectionHides) ? 0.3 : 1;
            element.fadeTo(FADE_DURATION, targetOpacity);
            element.addClass('selected');
            selected.push(value.label);
          }
        },
        
        redraw = function() {
          var selectedOpacity = (selectionHides) ? 0.3 : 1,
              selectedRows = list.find('li.selected'),
              
              unselectedOpacity = (selectionHides) ? 1 : 0.3,
              unselectedRows = list.find('li').not('.selected');

          selectedRows.fadeTo(FADE_DURATION, selectedOpacity); 
          unselectedRows.fadeTo(FADE_DURATION, unselectedOpacity); 
        },
        
        clear = function() {
          selected = [];
          list.removeClass();
          list.empty();
        },
        
        /** Shorthand function for sorting facet values by count **/
        sortFacetValues = function(a,b) { return b.count - a.count },
        
        editFilterSettings = function(facetValues) {
          var dim = facetValues.dimension,
              facets = facetValues.facets,
              maxCount = (facets.length > 0) ? facets.slice().sort(sortFacetValues)[0].count : 0;
              
          clear();
          list.addClass('chart large ' + dim);
 
          jQuery.each(facets, function(idx, val) {
            var label = Formatting.formatFacetLabel(val.label),
                tooltip = Formatting.formatNumber(val.count) + ' Results',
                percentage = 100 * val.count / maxCount,
                meter = Formatting.createMeter(label, tooltip, percentage);
                
            meter.prepend('<span class="icon selection-toggle">&#xf00d;</span>');
            meter.click(function() { select(meter, val); });
                
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
