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
          
        currentSelection = false,
        
        placeInfoBox = element.find('.place-info'),
        
        body = element.find('.body'),
        
        histogramSection = element.find('.section.histogram'),
        
        typeFacetSection = element.find('.section.facet.type'),
        
        sourceFacetSection = element.find('.section.facet.source'),
        
        footerTotals = element.find('.footer .total'),
        
        buttonToggleFilters = element.find('.advanced'),
        
        buttonListAll = element.find('.list-all'),
        
        timeHistogram, typeFacetChart, sourceFacetChart,
        
        toggleFilters = function() {
          var visible = body.is(':visible');
          body.slideToggle(200, function() {
            if (visible)
              buttonToggleFilters.removeClass('open');
            else
              buttonToggleFilters.addClass('open');
          });
        };
        
    /** Instantiate child controls **/
    body.hide();
    container.append(element);
    // timeHistogram = new TimeHistogram(histogramSection, eventBroker);
    
    typeFacetChart = new FacetChart(typeFacetSection, 'Type', 'type');
    sourceFacetChart = new FacetChart(sourceFacetSection, 'Source', 'dataset');
    
    buttonToggleFilters.click(toggleFilters);
    buttonListAll.click(function() { eventBroker.fireEvent(Events.UI_TOGGLE_ALL_RESULTS); });
    buttonListAll.mouseover(function() { 
      if (!currentSelection)
        eventBroker.fireEvent(Events.UI_SHOW_ALL_RESULTS); 
    });
    
    // We want to know about user selections, because as long as there is
    // no selection, mouseover should trigger 'the list all' action. Otherwise,
    // the user should have to click.
    eventBroker.addHandler(Events.UI_SELECT_PLACE, function(selection) {
      currentSelection = selection;
    });
    
    /** Forward updates to the facet charts **/
    eventBroker.addHandler(Events.API_SEARCH_SUCCESS, function(response) {
      footerTotals.html('(' + numeral(response.total).format('0,0') + ')'); 
      
      var facets = response.facets, 
          typeDimension = jQuery.grep(facets, function(facet) { return facet.dimension === 'type'; }),
          typeFacets = (typeDimension.length > 0) ? typeDimension[0].top_children : [],
          
          sourceDim = jQuery.grep(facets, function(facet) { return facet.dimension === 'source_dataset'; });
          sourceFacets = (sourceDim.length > 0) ? sourceDim[0].top_children : [];
          
      typeFacetChart.update(typeFacets);
      sourceFacetChart.update(sourceFacets);
    });

  };
  
  return FilterPanel;
    
});
