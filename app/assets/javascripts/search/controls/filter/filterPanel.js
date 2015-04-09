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
          '    <span class="list-all"><span class="icon">&#xf03a;</span> <span class="label">List all results</span></span>' +
          '    <span class="total">&nbsp;</span>' +
          '    <span class="advanced">Filters</span>' +
          '  </div>' +
          '</div>'),
        
        placeInfoBox = element.find('.place-info'),
        
        body = element.find('.body'),
        
        histogramSection = element.find('.section.histogram'),
        
        typeFacetSection = element.find('.section.facet.type'),
        
        sourceFacetSection = element.find('.section.facet.source'),
        
        footerTotals = element.find('.footer .total'),
        
        buttonToggle = element.find('.advanced'),
        
        buttonListAll = element.find('.list-all'),
        
        timeHistogram, typeFacetChart, sourceFacetChart,
        
        toggle = function() {
          var visible = jQuery(body).is(':visible');
          body.slideToggle(200, function() {
            if (visible)
              buttonToggle.removeClass('open');
            else
              buttonToggle.addClass('open');
          });
        };
        
    /** Instantiate child controls **/
    body.hide();
    container.append(element);
    timeHistogram = new TimeHistogram(histogramSection, eventBroker);
    
    typeFacetChart = new FacetChart(typeFacetSection, 'Type', 'type');
    sourceFacetChart = new FacetChart(sourceFacetSection, 'Source', 'dataset');
    
    buttonToggle.click(toggle);
    buttonListAll.click(function() { eventBroker.fireEvent(Events.LIST_ALL_RESULTS); });
    
    /** Forward updates to the facet charts **/
    eventBroker.addHandler(Events.UPATED_COUNTS, function(response) {
      var facets = response.facets, 
          typeDimension = jQuery.grep(facets, function(facet) { return facet.dimension === 'type'; }),
          typeFacets = (typeDimension.length > 0) ? typeDimension[0].top_children : [],
          
          sourceDim = jQuery.grep(facets, function(facet) { return facet.dimension === 'dataset'; });
          sourceFacets = (sourceDim.length > 0) ? sourceDim[0].top_children : [];
          
      footerTotals.html('(' + numeral(response.total).format('0,0') + ')');    
      
      typeFacetChart.update(typeFacets);
      sourceFacetChart.update(sourceFacets);
    });
  };
  
  return FilterPanel;
    
});
