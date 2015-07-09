require(['peripleo-ui/api/api',
         'peripleo-ui/controls/settings/settingsEditor',
         'peripleo-ui/controls/resultList',
         'peripleo-ui/controls/searchPanel',
         'peripleo-ui/controls/toolbar', 
         'peripleo-ui/events/events',
         'peripleo-ui/events/eventBroker',
         'peripleo-ui/events/lifecycleWatcher',
         'peripleo-ui/map/map', 
         'peripleo-ui/urlBar'], function(API, SettingsEditor, ResultList, SearchPanel, Toolbar, Events, EventBroker, LifeCycleWatcher, Map, URLBar) {
  
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
        
        /** resolve the URL bar **/
        initialSettings = urlBar.parseURLHash(window.location.hash),
        
        /** Is there an initially selected place? Fetch it now! **/       
        fetchInitiallySelectedPlace = function(encodedURL) {
          jQuery.getJSON('/peripleo/places/' + encodedURL, function(response) {
            eventBroker.fireEvent(Events.SELECT_RESULT, [ response ]);
          });
        };
        
    if (initialSettings) {
      // Set up UI with initial settings
      eventBroker.fireEvent(Events.LOAD, initialSettings);
      
      if (initialSettings.places)
        fetchInitiallySelectedPlace(initialSettings.places);

    } else {
      // Just start with a plain canvas
      eventBroker.fireEvent(Events.LOAD, { bbox: map.getBounds() });
    }
    
  });
  
});
