/** One 'facet dimension chart' block **/
define(['common/formatting', 'peripleo-ui/events/events', 'peripleo-ui/controls/filter/facetFilterParser'], function(Formatting, Events, FacetFilterParser) {
  
  var FacetChart = function(parent, title, dimension, eventBroker) {
    var header = jQuery(
          '<div class="facet-header">' +
          '  <h3>' + title + '</h3>' +
          '  <span class="filter-buttons">' +
          '    <a href="#" class="btn set-filter"><span class="icon">&#xf0b0;</span> <span class="label">Set Filter</span></a>' +
          '    <a href="#" class="btn refine"><span class="icon">&#xf0b0;</span> <span class="label">Refine</span></a>' +
          '    <a href="#" class="btn clear"><span class="icon">&#xf00d;</span> <span class="label">Clear</span></a>' +
          '  </span>' +
          '</div>'),
          
        btnSetFilter = header.find('.btn.set-filter'),
        btnRefine = header.find('.btn.refine'),
        btnClear = header.find('.btn.clear'),
        
        /** Current filter settings for this chart **/
        currentFilters = false,
          
        list = jQuery(
          '<ul class="chart ' + dimension + '"></ul>'),
          
        facets = [],
          
        /** Shorthand function for sorting facet values by count **/
        sortFacetValues = function(a,b) { return b.count - a.count },
          
        update = function(updatedFacets) {
          var maxCount = (updatedFacets.length > 0) ? updatedFacets.slice().sort(sortFacetValues)[0].count : 0;
              
          facets = updatedFacets;
          list.empty();
          
          jQuery.each(updatedFacets.slice(0, 5), function(idx, val) {
            var label = Formatting.formatFacetLabel(val.label),
                tooltip = Formatting.formatNumber(val.count) + ' Results',
                percentage = 100 * val.count / maxCount; 
                
            list.append(Formatting.createMeter(label, tooltip, percentage));
          });
        },
        
        /** Helper that removes 'falsy' properties from an object **/
        pruneEmpty = function(obj) {
          var copy = jQuery.extend({}, obj);
          
          for (var k in copy) {
            if (!copy[k])
              delete copy[k];
          }
          
          if (jQuery.isEmptyObject(copy))
            return false;
          else
            return copy;
        },
                
        /** Monitor if the user set or removed a filter on this dimension **/
        onSettingsChanged = function(settings) {
          if (settings.dimension === dimension) {
            if (settings.filters) {
              currentFilters = pruneEmpty(settings.filters);
              btnSetFilter.hide();
              btnRefine.show();
              btnClear.show();
            } else {
              onClear();
            }
          }
        },
        
        onClear = function() {
          currentFilters = false;
          btnRefine.hide();
          btnClear.hide();
          btnSetFilter.show();
          eventBroker.fireEvent(Events.SEARCH_CHANGED, FacetFilterParser.parse(dimension));
        };
    
    btnRefine.hide();
    btnClear.hide();
    
    btnSetFilter.add(btnRefine).click(function() {
      eventBroker.fireEvent(Events.EDIT_FILTER_SETTINGS, { dimension: dimension, facets: facets, currentFilters: currentFilters });
      return false;
    });    
    
    btnClear.click(function() {
      onClear();
      eventBroker.fireEvent(Events.CLEAR_FILTER_SETTINGS, { dimension: dimension });
      return false;
    });
    
    parent.append(header);
    parent.append(list);

    eventBroker.addHandler(Events.FILTER_SETTINGS_CHANGED, onSettingsChanged);
    
    this.update = update;
  };
  
  return FacetChart;
  
});
