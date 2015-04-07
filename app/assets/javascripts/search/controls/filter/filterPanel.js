/** One 'facet dimension chart' block **/
define(['search/events', 
        'search/controls/filter/timeHistogram',
        'search/controls/filter/facetChart'], function(Events, TimeHistogram, FacetChart) {
  
  var FilterPanel = function(container, eventBroker) {
    var element = jQuery(
          '<div id="filterpanel">' +
          '  <div class="body">' +
          '    <div class="section histogram"></div>' +
          '    <div class="section facet type"></div>' +
          '    <div class="section facet source"></div>' +
          '  </div>' + 
          '  <div class="footer">' +
          '    <span class="total">&nbsp;</span>' +
          '    <span class="icon close">&#xf077;</span>' +
          '  </div>' +
          '</div>'),
        
        placeInfoBox = element.find('.place-info'),
        
        body = element.find('.body'),
        
        histogramSection = element.find('.section.histogram'),
        
        typeFacetSection = element.find('.section.facet.type'),
        
        sourceFacetSection = element.find('.section.facet.source'),
        
        footerTotals = element.find('.footer .total'),
        
        buttonToggle = element.find('.icon.close'),
        
        timeHistogram, typeFacetChart, sourceFacetChart,
        
        toggle = function() {
          var visible = jQuery(body).is(':visible');
          body.slideToggle(200, function() {
            if (visible)
              buttonToggle.html('&#xf078;');
            else
              buttonToggle.html('&#xf077;');
          });
        };
        
    /** Instantiate child controls **/
    container.append(element);
    timeHistogram = new TimeHistogram(histogramSection, eventBroker);
    
    typeFacetChart = new FacetChart(typeFacetSection, 'Object Types', 'type');
    sourceFacetChart = new FacetChart(sourceFacetSection, 'Source Datasets', 'dataset');
    
    buttonToggle.click(toggle);
    
    /** Forward updates to the facet charts **/
    eventBroker.addHandler(Events.UPATED_COUNTS, function(response) {
      var facets = response.facets, 
          typeDimension = jQuery.grep(facets, function(facet) { return facet.dimension === 'type'; }),
          typeFacets = (typeDimension.length > 0) ? typeDimension[0].top_children : [],
          
          sourceDim = jQuery.grep(facets, function(facet) { return facet.dimension === 'dataset'; });
          sourceFacets = (sourceDim.length > 0) ? sourceDim[0].top_children : [];
          
      footerTotals.html(numeral(response.total).format('0,0') + ' Results');    
      
      typeFacetChart.update(typeFacets);
      sourceFacetChart.update(sourceFacets);
    });
  };
  
  return FilterPanel;
    
});
