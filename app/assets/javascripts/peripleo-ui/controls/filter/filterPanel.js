/** One 'facet dimension chart' block **/
define(['common/formatting',
        'peripleo-ui/controls/filter/facetChart',
        'peripleo-ui/controls/filter/filterEditor',
        'peripleo-ui/controls/filter/timeHistogram',
        'peripleo-ui/events/events'
        ], function(Formatting, FacetChart, FilterEditor, TimeHistogram, Events) {
          
  var SLIDE_DURATION = 180,
  
      /** Enum for search states **/
      SearchState = { SEARCH : 1, SUB_SEARCH : 2 };
  
  var FilterPanel = function(container, eventBroker) {
    
        /** Slide-able body section **/
    var body = jQuery(
          '<div class="body">' +
          '  <div class="section histogram"></div>' +
          '  <div class="section facet type"></div>' +
          '  <div class="section facet source"></div>' +
          '</div>'),
        
        /** Footer (remains visible when panel slides in) **/
        footer = jQuery(
          '<div class="footer">' +
          '  <span class="list-all"><span class="icon">&#xf03a;</span> <span class="label">Show all results</span></span>' +
          '  <span class="total">&nbsp;</span>' +
          '  <span class="advanced">Filters</span>' +
          '</div>'),
        
        /** DOM element shorthands **/
        histogramSection = body.find('.section.histogram'),
        typeFacetSection = body.find('.section.facet.type'),
        sourceFacetSection = body.find('.section.facet.source'),
        footerLabel = footer.find('.label'),
        footerTotals = footer.find('.total'),
        buttonListAll = footer.find('.list-all'),
        buttonToggleFilters = footer.find('.advanced'),
        
        /** Sub-components - to be initialized after body added to DOM **/
        timeHistogram,
        typeFacetChart,
        sourceFacetChart,
        
        /** The current search state defines if the footer shows total or related results **/
        currentSearchState = SearchState.SEARCH,
        
        /** Stores current total result count **/
        currentTotals = 0,
        
        /** Flag caching the state of the 'Show results' button **/
        resultsShown = false,
        
        /** We need to ignore the automatic view update following a subsearch **/
        ignoreNextViewUpdate = false,
        
        /** Filter editor **/
        filterEditor = filterEditor = new FilterEditor(eventBroker),
        
        /** Slides the panel in or out **/
        togglePanel = function() {
          var visible = body.is(':visible'),
              action = (visible) ? 'slideUp' : 'slideDown';
          
          if (visible)
            eventBroker.fireEvent(Events.HIDE_FILTERS);
          else
            eventBroker.fireEvent(Events.SHOW_FILTERS);
          
          body.velocity(action, { 
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
        
        /** Refreshes the charts **/
        refresh = function(response) { 
          var facets = response.facets, 
              typeDimension = jQuery.grep(facets, function(facet) { return facet.dimension === 'type'; }),
              typeFacets = (typeDimension.length > 0) ? typeDimension[0].top_children : [],
          
              sourceDim = jQuery.grep(facets, function(facet) { return facet.dimension === 'source_dataset'; });
              sourceFacets = (sourceDim.length > 0) ? sourceDim[0].top_children : [];
           
          currentTotals = response.total;
          footerTotals.html('(' + Formatting.formatNumber(currentTotals) + ')');
          
          timeHistogram.update(response);
          typeFacetChart.update(typeFacets);
          sourceFacetChart.update(sourceFacets);
        },
        
        /** Switch panel to 'search' state **/
        toStateSearch = function() {
          currentSearchState = SearchState.SEARCH;
          footerLabel.html('Show all results');
          footerTotals.html('(' + Formatting.formatNumber(currentTotals) + ')');
        },
        
        /** Switch panel to 'subsearch' state **/
        toStateSubsearch = function(subsearch) { 
          // TODO multi-selection?
          var firstPlace = subsearch.places[0];
          
          resultsShown = true;
          ignoreNextViewUpdate = true;
          
          currentSearchState = SearchState.SUB_SEARCH;
          footerLabel.html('Show results at ' + firstPlace.title);
          footerTotals.html(' (' + Formatting.formatNumber(subsearch.total) + ')');
        },
        
        /** Sets the 'list results' button to state where click triggers open **/
        resultsButtonToStateShow = function() {
          if (currentSearchState === SearchState.SEARCH) {
            footerLabel.html('Show all results');
          } else {
            // Change start to 'Show r...'
            footerLabel.html('Show r' + footerLabel.html().substr(1));
          }
        },
        
        /** Sets the 'list results' button to state where click triggers open **/
        resultsButtonToStateHide = function() {
          if (currentSearchState === SearchState.SEARCH) {
            footerLabel.html('All results');
          } else {            
            // Strip 'Show r...' from start (and add capital R)
            footerLabel.html('R' + footerLabel.html().substr(6));
          }
        };

    // Instantiate child controls
    body.hide();
    container.append(body);
    container.append(footer);
    
    timeHistogram = new TimeHistogram(histogramSection, eventBroker);
    typeFacetChart = new FacetChart(typeFacetSection, 'Type', 'type', eventBroker);
    sourceFacetChart = new FacetChart(sourceFacetSection, 'Source', 'source_dataset', eventBroker);
    
    buttonToggleFilters.click(togglePanel);
    buttonListAll.click(function() { 
      if (resultsShown) {
        resultsButtonToStateShow();
        eventBroker.fireEvent(Events.HIDE_RESULTS); 
      } else {
        resultsButtonToStateHide();
        if (currentSearchState === SearchState.SEARCH)
          eventBroker.fireEvent(Events.SHOW_ALL_RESULTS); 
        else
          eventBroker.fireEvent(Events.SHOW_SUBSEARCH_RESULTS);
      }
      resultsShown = !resultsShown;      
    });

    // Refresh on initial load
    eventBroker.addHandler(Events.API_INITIAL_RESPONSE, refresh);
    
    // Refresh on view updates, unless we're currently in a subsearch
    eventBroker.addHandler(Events.API_VIEW_UPDATE, function(response) {
      if (!ignoreNextViewUpdate && resultsShown) {
        resultsButtonToStateShow();
        resultsShown = false;
      } else {
        ignoreNextViewUpdate = false;
      }
      
      if (currentSearchState === SearchState.SEARCH)
        refresh(response);
    });
    
    eventBroker.addHandler(Events.SELECTION, function() {
      if (resultsShown)
        resultsButtonToStateShow();
    });
    
    // Refresh on subsearch response
    eventBroker.addHandler(Events.API_SUB_SEARCH_RESPONSE, function(response) { 
      refresh(response);
      resultsButtonToStateHide();
      resultsShown = true;
    });
    
    // Footer displays different contents in 'search' and 'subsearch' states
    eventBroker.addHandler(Events.TO_STATE_SUB_SEARCH, toStateSubsearch);
    eventBroker.addHandler(Events.TO_STATE_SEARCH, toStateSearch);
  };
  
  return FilterPanel;
    
});

