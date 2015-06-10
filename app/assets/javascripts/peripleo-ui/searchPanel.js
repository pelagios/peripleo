/** 
 * The main search control container.
 */
define(['peripleo-ui/events/events',
        'peripleo-ui/controls/autoSuggest',
        'peripleo-ui/controls/filterPanel',
        'peripleo-ui/controls/selectionInfo',
        'peripleo-ui/controls/searchAtButton'], function(Events, AutoSuggest, FilterPanel, SelectionInfo, SearchAtButton) {
  
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
          '      <span id="search-icon" class="icon">&#xf002;</span>' +
          '      <div id="button-listall"><span class="icon">&#xf03a;</span> List all results</div>' +
          '    </form>' +
          '  </div>' +
          '  <div id="filterpanel"></div>' +
          '  <div id="selection-info"></div>' +
          '  <div id="button-search-at"></div>' +  
          '</div>'),
                    
        /** DOM element shorthands **/
        searchForm = element.find('form'),
        searchInput = searchForm.find('input'),
        searchIcon = element.find('#search-icon'),
        btnListAll = element.find('#button-listall'),
        
        filterPanelContainer = element.find('#filterPanel'),
        selectionInfoContainer = element.find('#selection-info'),
        searchAtContainer = element.find('#button-search-at'),
        
        /** Sub-elements - to be initialized after element was added to DOM **/
        autoSuggest, filterPanel, selectionInfo, searchAtButton,
       
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
    
        /** Handler for the 'X' clear button **/
        onClearSearch = function() {
          autoSuggest.clear();
          searchForm.submit();
          updateIcon();
        },
        
        /** Switch to 'search' state **/
        toStateSearch = function() {
          btnListAll.hide();
          filterPanelContainer.insertBefore(selectionInfoContainer);
        },
        
        /** Switch to 'subsearch' state **/
        toStateSubsearch = function(places) {
          btnListAll.show();
          selectionInfoContainer.insertBefore(filterPanelContainer);
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
       
    searchForm.on('click', '.clear', onClearSearch);
    
    // Flat 'list-all' button only shown in subsearch state
    btnListAll.hide();
        
    // Append panel to the DOM
    container.append(element);
    autoSuggest = new AutoSuggest(searchForm, searchInput);
    filterPanel = new FilterPanel(filterPanelContainer, eventBroker);
    selectionInfo = new SelectionInfo(selectionInfoContainer, eventBroker);
    searchAtButton = new SearchAtButton(searchAtContainer, eventBroker);
    
    // Fill with intial query, if any
    eventBroker.addHandler(Events.LOAD, function(initialSettings) {
      if (initialSettings.query) {
        input.val(initialSettings.query);
        updateIcon();
      }
    });
    
    eventBroker.addHandler(Events.SUB_SEARCH, toStateSubsearch);
    eventBroker.addHandler(Events.SELECTION, toStateSearch);
  };
  
  return SearchPanel;
  
});
