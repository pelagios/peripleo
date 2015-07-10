require(['peripleo-ui/controls/settings/settingsEditor',
         'peripleo-ui/controls/resultList',
         'peripleo-ui/controls/searchPanel',
         'peripleo-ui/controls/toolbar', 
         'peripleo-ui/events/events',
         'peripleo-ui/events/eventBroker',
         'peripleo-ui/events/lifecycleWatcher',
         'peripleo-ui/map/map',
         'peripleo-ui/api', 
         'peripleo-ui/urlBar'], function(SettingsEditor, ResultList, SearchPanel, Toolbar, Events, EventBroker, LifeCycleWatcher, Map, API, URLBar) {
  
  jQuery(document).ready(function() {  
        /** DOM element shorthands **/
    var mapDIV = document.getElementById('map'),
        controlsDIV = jQuery('#controls'),
        toolbarDIV = jQuery('#toolbar'),

        /** Top-level components **/
        eventBroker = new EventBroker(),
        lifeCycleWatcher = new LifeCycleWatcher(eventBroker);
        urlBar = new URLBar(eventBroker),
        api = new API(eventBroker),
        map = new Map(mapDIV, eventBroker),
        toolbar = new Toolbar(toolbarDIV, eventBroker),
        searchPanel = new SearchPanel(controlsDIV, eventBroker),
        resultList = new ResultList(controlsDIV, eventBroker),
        settingsEditor = new SettingsEditor(eventBroker),
        
        /** Resolve the URL bar **/
        initialSettings = urlBar.parseURLHash(window.location.hash),
        
        /** Is there an initially selected place? Fetch it now! **/       
        fetchInitiallySelectedPlace = function(encodedURL, zoomTo) {
          jQuery.getJSON('/peripleo/places/' + encodedURL, function(response) {
            eventBroker.fireEvent(Events.SELECT_RESULT, [ response ]);
            if (zoomTo)
              map.zoomTo(response.geo_bounds);
          });
        },
        
        /** Initializes map center and zoom **/
        initializeMap = function(at) {
          map.setView([ at.lat, at.lng ], at.zoom);
        };
            
    if (initialSettings.at)
      initializeMap(initialSettings.at);
              
    eventBroker.fireEvent(Events.LOAD, initialSettings);
    
    if (initialSettings.places)
      fetchInitiallySelectedPlace(initialSettings.places, !initialSettings.hasOwnProperty('at'));

    delete initialSettings.at;
  });
  
});
