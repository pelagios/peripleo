/** 
 * The main search control container.
 */
define(['common/formatting',
        'peripleo-ui/controls/filter/filterPanel',
        'peripleo-ui/controls/selection/selectedItem',
        'peripleo-ui/controls/selection/selectedPlace',
        'peripleo-ui/controls/autoSuggest',
        'peripleo-ui/controls/searchAtButton',
        'peripleo-ui/events/events'], function(Formatting, FilterPanel, SelectedItem, SelectedPlace, AutoSuggest, SearchAtButton, Events) {
  
  var SLIDE_DURATION = 180;
  
  var SearchPanel = function(container, eventBroker) {
    
        /** 
         * A container DIV holding:
         * - the search form
         * - the flat 'List All' button shown while UI is in subsearch state
         * - a container DIV for the filter panel
         * - container DIV for the selection info, in default (search state) position
         * - a container DIV for the 'search at' subsearch button.
         */
    var element = jQuery(
          '<div id="searchpanel-container">' +
          '  <div id="searchbox">' +
          '    <form>' +
          '      <input type="text" id="query" name="query" autocomplete="off">' +
          '      <span id="subsearch-indicator" class="icon">&#xf041;</span>' +
          '      <span id="search-icon" class="icon">&#xf002;</span>' +
          '      <div id="button-listall">' +
          '        <span class="list-all"><span class="icon">&#xf03a;</span> <span class="label">Show all results</span></span>' +
          '        <span class="total">&nbsp;</span>' +
          '      </div>' +
          '    </form>' +
          '  </div>' +
          '  <div id="filterpanel"></div>' +
          '  <div id="selected-place" class="selection-info"></div>' +
          '  <div id="button-search-at"></div>' +  
          '  <div id="selected-item" class="selection-info"></div>' +
          '</div>'),
                    
        /** DOM element shorthands **/
        searchForm = element.find('form'),
        searchInput = searchForm.find('input'),
        subsearchIndicator = searchForm.find('#subsearch-indicator'),
        searchIcon = searchForm.find('#search-icon'),
        resultsButton = searchForm.find('#button-listall'),
        resultsLabel = resultsButton.find('.label'),
        listAllTotals = resultsButton.find('.total'),
        
        filterPanelContainer = element.find('#filterPanel'),
        selectedPlaceContainer = element.find('#selected-place'),
        searchAtContainer = element.find('#button-search-at'),
        selectedItemContainer = element.find('#selected-item'),
        
        /** Sub-elements - to be initialized after element was added to DOM **/
        autoSuggest, filterPanel, selectedPlace, searchAtButton, selectedItem,
        
        /** Shorthand flag indicating whether the current state is 'subsearch' **/
        isStateSubsearch = false,
        
        /** Flag caching the state of the 'Show results' button **/
        resultsShown = false,
        
        /** Stores current total result count **/
        currentTotals = 0,
       
        /** Updates the icon according to the contents of the search input field **/
        updateIcon = function() {
          var chars = searchInput.val().trim();
      
          if (chars.length === 0 && !isStateSubsearch) {
            searchIcon.html('&#xf002;');
            searchIcon.removeClass('clear');
          } else {
            searchIcon.html('&#xf00d;');
            searchIcon.addClass('clear');
          }
        },
        
        /** Updates the result totals count HTML field **/
        updateTotalsCount = function() {
          listAllTotals.html('(' + Formatting.formatNumber(currentTotals) + ')');
        },
        
        /** Handler for the 'X' clear button **/
        onResetSearch = function() {
          eventBroker.fireEvent(Events.TO_STATE_SEARCH);
          autoSuggest.clear();
          searchForm.submit();
          updateIcon();
        },
        
        /** We keep the total search result count for display in the flat 'List All' button **/
        onViewUpdate = function(response) {
          resetResultsButton();
          currentTotals = response.total;
          if (isStateSubsearch)
            updateTotalsCount();
        },
        
        /** Switch to 'search' state **/
        toStateSearch = function() {
          isStateSubsearch = false;
          subsearchIndicator.hide();
          searchInput.removeClass('search-at');
          resultsButton.hide();
          filterPanelContainer.insertBefore(selectedPlaceContainer);
        },
        
        /** Switch to 'subsearch' state **/
        toStateSubsearch = function(subsearch) {      
          isStateSubsearch = true;
          resetResultsButton();
          
          // Sub-search may remove the query phrase - update accordingly
          if (subsearch.clear_query) {
            searchInput.val('');
            updateIcon();
          }
          
          // Show indicator icon
          subsearchIndicator.show();
          searchInput.addClass('search-at');
          searchInput.focus();
          
          updateIcon();
          
          // Update footer 
          updateTotalsCount();
          resultsButton.show();
          filterPanelContainer.slideUp(SLIDE_DURATION, function() {
            selectedPlaceContainer.insertBefore(filterPanelContainer);
            filterPanelContainer.velocity('slideDown', { duration: SLIDE_DURATION });            
          });
        },
        
        resetResultsButton = function() {
          if (resultsShown) {
            resultsLabel.html('Show all results');
            resultsShown = false;           
          }
        },
        
        onClickResultsButton = function() {
          if (resultsShown) {
            resultsLabel.html('Show all results');
            resultsShown = false;
            eventBroker.fireEvent(Events.HIDE_RESULTS);
          } else {
            resultsLabel.html('All results');
            resultsShown = true;
            eventBroker.fireEvent(Events.SHOW_ALL_RESULTS);
          }
        };
    
    
    // Set up events
    searchForm.submit(function(e) {
      var chars = searchInput.val().trim();

      if (chars.length === 0)
        eventBroker.fireEvent(Events.SEARCH_CHANGED, { query : false });
      else
        eventBroker.fireEvent(Events.SEARCH_CHANGED, { query : chars });
    
      searchInput.blur();
      return false; // preventDefault + stopPropagation
    });
    
    searchForm.keypress(function (e) {
      updateIcon();
      if (e.which == 13) {
        searchForm.submit();
        return false; // Otherwise we'll get two submit events
      }
    });
       
    searchForm.on('click', '.clear', onResetSearch);
    
    // Subsearch indicator and 'list-all' button only shown in subsearch state
    subsearchIndicator.hide();
    resultsButton.click(onClickResultsButton);    
    resultsButton.hide();
        
    // Append panel to the DOM
    container.append(element);
    autoSuggest = new AutoSuggest(searchForm, searchInput);
    filterPanel = new FilterPanel(filterPanelContainer, eventBroker);
    selectedPlace = new SelectedPlace(selectedPlaceContainer, eventBroker);
    searchAtButton = new SearchAtButton(searchAtContainer, eventBroker);
    selectedItem = new SelectedItem(selectedItemContainer, eventBroker);
    
    // Fill with intial query, if any
    eventBroker.addHandler(Events.LOAD, function(initialSettings) {
      if (initialSettings.query) {
        searchInput.val(initialSettings.query);
        updateIcon();
      }
    });

    eventBroker.addHandler(Events.API_VIEW_UPDATE, onViewUpdate);    
    eventBroker.addHandler(Events.API_INITIAL_RESPONSE, onViewUpdate); // Just re-use view update handler
    
    eventBroker.addHandler(Events.SHOW_SUBSEARCH_RESULTS, resetResultsButton);
    
    eventBroker.addHandler(Events.TO_STATE_SUB_SEARCH, toStateSubsearch);
    eventBroker.addHandler(Events.TO_STATE_SEARCH, toStateSearch);
  };
  
  return SearchPanel;
  
});
