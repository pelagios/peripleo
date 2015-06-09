/** One 'facet dimension chart' block **/
define(['peripleo-ui/events/events', 
        'peripleo-ui/controls/filter/timeHistogram',
        'peripleo-ui/controls/filter/facetChart'], function(Events, TimeHistogram, FacetChart) {
          
  var SLIDE_DURATION = 120;
  
  var FilterPanel = function(parent, eventBroker) {
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
          
          if (visible)
            eventBroker.fireEvent(Events.HIDE_FILTERS);
          else
            eventBroker.fireEvent(Events.SHOW_FILTERS);
          
          body.slideToggle({ 
            duration: SLIDE_DURATION, 
            
            step: function() { eventBroker.fireEvent(Events.CONTROLS_ANIMATION); },
            
            complete: function() {
              if (visible)
                buttonToggleFilters.removeClass('open');
              else
                buttonToggleFilters.addClass('open');
              
              eventBroker.fireEvent(Events.CONTROLS_ANIMATION_END);
            }
          });
        },
        
        render = function(response) {      
          var facets = response.facets, 
              typeDimension = jQuery.grep(facets, function(facet) { return facet.dimension === 'type'; }),
              typeFacets = (typeDimension.length > 0) ? typeDimension[0].top_children : [],
          
              sourceDim = jQuery.grep(facets, function(facet) { return facet.dimension === 'source_dataset'; });
              sourceFacets = (sourceDim.length > 0) ? sourceDim[0].top_children : [];
           
          footerTotals.html('(' + numeral(response.total).format('0,0') + ')');
          
          typeFacetChart.update(typeFacets);
          sourceFacetChart.update(sourceFacets);
        };
        
    /** Instantiate child controls **/
    body.hide();
    parent.append(element);
    timeHistogram = new TimeHistogram(histogramSection, eventBroker);
    
    typeFacetChart = new FacetChart(typeFacetSection, 'Type', 'type', eventBroker);
    sourceFacetChart = new FacetChart(sourceFacetSection, 'Source', 'source_dataset', eventBroker);
    
    buttonToggleFilters.click(toggleFilters);
    buttonListAll.click(function() { eventBroker.fireEvent(Events.TOGGLE_ALL_RESULTS); });
    
    // We want to know about user selections, because as long as there is
    // no selection, mouseover should trigger 'the list all' action. Otherwise,
    // the user should have to click.
    eventBroker.addHandler(Events.SELECTION, function(selection) {
      currentSelection = selection;
    });
    
    // Forward updates to the facet charts
    eventBroker.addHandler(Events.API_INITIAL_RESPONSE, render);
    eventBroker.addHandler(Events.API_VIEW_UPDATE, render);

  };
  
  return FilterPanel;
    
});
