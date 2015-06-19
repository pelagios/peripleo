/** A helper that manages 'derivative events' in the Peripleo UI lifecycle **/
define(['peripleo-ui/events/events'], function(Events) {
  
  var LifecycleWatcher = function(eventBroker) {
    
    /** Flag recording current search mode state **/
    var isStateSubsearch = false;
    
    /** SELECT_MARKER & SELECT_RESULT trigger parent SELECTION **/
    eventBroker.addHandler(Events.SELECT_MARKER, function(selection) { 
      eventBroker.fireEvent(Events.SELECTION, selection);
    });
    
    eventBroker.addHandler(Events.SELECT_RESULT, function(results) { 
      eventBroker.fireEvent(Events.SELECTION, results);
    });
    
    /** 
     * When in subsearch state, selection of a new place or place de-selection
     * triggers TO_STATE_SEARCH
     */
    eventBroker.addHandler(Events.TO_STATE_SUB_SEARCH, function() {
      isStateSubsearch = true;  
    });
    
    eventBroker.addHandler(Events.SELECT_MARKER, function(places) {
      eventBroker.fireEvent(Events.TO_STATE_SEARCH);
    });
    
  };
  
  return LifecycleWatcher;
  
});
