/** One 'facet dimension chart' block **/
define(['search/events', 
        'search/controls/filter/timeHistogram',
        'search/controls/filter/facetChart'], function(Events, TimeHistogram, FacetChart) {
  
  var FilterPanel = function(container, eventBroker) {
    var element = jQuery(
          '<div id="filterpanel">' +
          '  <div class="header"></div>' +
          '  <div class="section histogram"></div>' +
          '  <div class="section facets"><table></table></div>' +
          '</div>'),
        
        header = element.find('.header'),
        
        histogramSection = element.find('.section.histogram'),
        
        facetSection = element.find('.section.facets table'),
        
        timeHistogram, typeFacetChart, datasourceFacetChart; 

    /** Instantiate child controls **/
    container.append(element);
    timeHistogram = new TimeHistogram(histogramSection, eventBroker);
    
    typeFacetChart = new FacetChart(facetSection, 'Object Types', 'type');
    datasourceFacetChart = new FacetChart(facetSection, 'Source Datasets', 'source');
    
    /** Forward updates to the facet charts **/
    eventBroker.addHandler(Events.UPATED_COUNTS, function(response) {
      var facets = response.facets, 
          typeDimension = jQuery.grep(facets, function(facet) { return facet.dimension === 'type'; }),
          typeFacets = (typeDimension.length > 0) ? typeDimension[0].top_children : [],
          
          sourceDim = jQuery.grep(facets, function(facet) { return facet.dimension === 'dataset'; });
          sourceFacets = (sourceDim.length > 0) ? sourceDim[0].top_children : [];
          
      header.html(numeral(response.total).format('0,0') + ' Results');    
      
      typeFacetChart.update(typeFacets);
      datasourceFacetChart.update(sourceFacets);
    });
  };
  
  return FilterPanel;
    
});
