/** 
 * The main search control container.
 */
define(['common/formatting',
        'peripleo-ui/controls/filter/filterPanel',
        'peripleo-ui/controls/selection/selectedPlace',
        'peripleo-ui/controls/autoSuggest',
        'peripleo-ui/controls/searchAtButton',
        'peripleo-ui/events/events'], function(Formatting, FilterPanel, SelectedPlace, AutoSuggest, SearchAtButton, Events) {
  
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
          '</div>'),
                    
        /** DOM element shorthands **/
        searchForm = element.find('form'),
        searchInput = searchForm.find('input'),
        subsearchIndicator = searchForm.find('#subsearch-indicator'),
        searchIcon = searchForm.find('#search-icon'),
        btnListAll = searchForm.find('#button-listall'),
        listAllTotals = btnListAll.find('.total'),
        
        filterPanelContainer = element.find('#filterPanel'),
        selectedPlaceContainer = element.find('#selected-place'),
        searchAtContainer = element.find('#button-search-at'),
        
        /** Sub-elements - to be initialized after element was added to DOM **/
        autoSuggest, filterPanel, selectedPlace, searchAtButton,
        
        /** Shorthand flag indicating whether the current state is 'subsearch' **/
        isStateSubsearch = false,
        
        /** Stores current total result count **/
        currentTotals = 0,
       
        /** Updates the icon according to the contents of the search input field **/
        updateIcon = function() {
          var chars = searchInput.val().trim();
      
          if (chars.length === 0) {
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
          autoSuggest.clear();
          searchForm.submit();
          updateIcon();
          eventBroker.fireEvent(Events.TO_STATE_SEARCH);
        },
        
        /** We keep the total search result count for display in the flat 'List All' button **/
        onViewUpdate = function(response) {
          currentTotals = response.total;
          if (isStateSubsearch)
            updateTotalsCount();
        },
        
        /** Switch to 'search' state **/
        toStateSearch = function() {
          isStateSubsearch = false;
          subsearchIndicator.hide();
          searchInput.removeClass('search-at');
          btnListAll.hide();
          filterPanelContainer.insertBefore(selectedPlaceContainer);
        },
        
        /** Switch to 'subsearch' state **/
        toStateSubsearch = function(subsearch) {          
          isStateSubsearch = true;
          
          // Sub-search may remove the query phrase - update accordingly
          if (subsearch.clear_query) {
            searchInput.val('');
            updateIcon();
          }
          
          // Show indicator icon
          subsearchIndicator.show();
          searchInput.addClass('search-at');
          searchInput.focus();
          
          // Update footer 
          updateTotalsCount();
          btnListAll.show();
          filterPanelContainer.slideUp(SLIDE_DURATION, function() {
            selectedPlaceContainer.insertBefore(filterPanelContainer);
            filterPanelContainer.slideDown(SLIDE_DURATION, function() {
              eventBroker.fireEvent(Events.CONTROLS_ANIMATION_END);
            });            
          });
        };
    
    
    // Set up events
    searchForm.submit(function(e) {
      var chars = searchInput.val().trim();

      if (chars.length === 0) {
        eventBroker.fireEvent(Events.QUERY_PHRASE_CHANGED, false);
        eventBroker.fireEvent(Events.SEARCH_CHANGED, { query : false });
      } else {
        eventBroker.fireEvent(Events.QUERY_PHRASE_CHANGED, chars);
        eventBroker.fireEvent(Events.SEARCH_CHANGED, { query : chars });
      }
    
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
    btnListAll.hide();
        
    // Append panel to the DOM
    container.append(element);
    autoSuggest = new AutoSuggest(searchForm, searchInput);
    filterPanel = new FilterPanel(filterPanelContainer, eventBroker);
    selectedPlace = new SelectedPlace(selectedPlaceContainer, eventBroker);
    searchAtButton = new SearchAtButton(searchAtContainer, eventBroker);
    
    // Fill with intial query, if any
    eventBroker.addHandler(Events.LOAD, function(initialSettings) {
      if (initialSettings.query) {
        searchInput.val(initialSettings.query);
        updateIcon();
      }
    });

    eventBroker.addHandler(Events.API_VIEW_UPDATE, onViewUpdate);    
    eventBroker.addHandler(Events.API_INITIAL_RESPONSE, onViewUpdate); // Just re-use view update handler
    
    eventBroker.addHandler(Events.TO_STATE_SUB_SEARCH, toStateSubsearch);
    eventBroker.addHandler(Events.SELECTION, toStateSearch);
  };
  
  return SearchPanel;
  
});
