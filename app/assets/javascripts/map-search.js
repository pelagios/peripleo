require(['search/map/map', 
         'search/controls/searchBox', 
         'search/controls/selection/selectionInfoBox',
         'search/controls/filter/filterPanel',
         'search/controls/filter/filterEditor',
         'search/controls/resultList',
         'search/urlBar',
         'search/api',
         'search/eventBroker',
         'search/events'], function(Map, SearchBox, SelectionInfoBox, FilterPanel, FilterEditor, ResultList, URLBar, API, EventBroker, Events) {
  
  jQuery(document).ready(function() {  
    var container = jQuery('#controls'),
    
        eventBroker = new EventBroker(),
        
        urlBar = new URLBar(eventBroker),
        
        api = new API(eventBroker),
        
        map = new Map(document.getElementById('map'), eventBroker),
        
        searchBox = new SearchBox(container, eventBroker),
        
        selectionInfoBox = new SelectionInfoBox(container, eventBroker),
        
        filterPanel = new FilterPanel(container, eventBroker),
        
        filterEditor = new FilterEditor(eventBroker),
        
        resultList = new ResultList(container, eventBroker);
        
    // Fire initial 'load' event
    eventBroker.fireEvent(Events.LOAD, map.getBounds());
        
  });
  
});
