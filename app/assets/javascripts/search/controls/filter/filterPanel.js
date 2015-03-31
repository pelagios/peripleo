/** One 'facet dimension chart' block **/
define(['search/events', 
        'search/controls/filter/timeHistogram',
        'search/controls/filter/facetChart'], function(Events, TimeHistogram, FacetChart) {
  
  var FilterPanel = function(container, eventBroker) {
    var element = jQuery(
          '<div id="filter-panel">' +
          '  <div id="facet-charts">' +
          '    <table></table>' +
          '  </div>' +
          '</div>'),
        
        facets = element.find('table'),
        
        timeHistogram, typeFacetChart, datasourceFacetChart; 

    /** Instantiate child controls **/
    container.append(element);
    timeHistogram = new TimeHistogram(element, eventBroker);
    
    typeFacetChart = new FacetChart(facets, 'Object Types');
    datasourceFacetChart = new FacetChart(facets, 'Source Datasets');
    
    /** Forward updates to the facet charts **/
    eventBroker.addHandler(Events.UPATED_COUNTS, function(response) {
      var facets = response.facets, 
          typeDimension = jQuery.grep(facets, function(facet) { return facet.dimension === 'type'; }),
          typeFacets = (typeDimension.length > 0) ? typeDimension[0].top_children : [],
          
          sourceDim = jQuery.grep(facets, function(facet) { return facet.dimension === 'dataset'; });
          sourceFacets = (sourceDim.length > 0) ? sourceDim[0].top_children : [];
          
      typeFacetChart.update(typeFacets);
      datasourceFacetChart.update(sourceFacets);
      
      // TODO update total count
    });
  };
  
  return FilterPanel;
    
});
