require(['search/map/map', 
         'search/controls/searchBox', 
         'search/controls/filter/filterPanel',
         'search/api',
         'search/eventBroker',
         'search/events'], function(Map, SearchBox, FilterPanel, API, EventBroker, Events) {
  
  jQuery(document).ready(function() {  
    var container = jQuery('#controls'),
    
        eventBroker = new EventBroker(),
        
        api = new API(eventBroker),
        
        map = new Map(document.getElementById('map'), eventBroker),
        
        searchBox = new SearchBox(container, eventBroker),
        
        filterPanel = new FilterPanel(container, eventBroker);
        
    // Fire initial 'load' event
    eventBroker.fireEvent(Events.LOAD, map.getBounds());
        
  });
  
});
