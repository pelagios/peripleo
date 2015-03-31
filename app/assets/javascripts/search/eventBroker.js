/** A generic event broker implementation **/
define(function() {
  
  var _handlers = [];
  
  /** A central event broker for communication between UI components **/
  var EventBroker = function(events) {
    
    this.events = events;
    
  };
  
  /** Adds an event handler **/
  EventBroker.prototype.addHandler = function(type, handler) {
    if (!_handlers[type])
      _handlers[type] = [];
      
    _handlers[type].push(handler);
  };
  
  /** Removes an event handler **/
  EventBroker.prototype.removeHandler = function(type, handler) {
    var handlers = _handlers[type];
    if (handlers) {
      var idx = handlers.indexOf(handler);
      handlers.splice(idx, 1);  
    }
  };   
  
  /** Fires an event **/
  EventBroker.prototype.fireEvent = function(type, opt_event) {
    var handlers = _handlers[type];
    if (handlers) {
      jQuery.each(handlers, function(idx, handler) {
        handler(opt_event);
      });
    }    
  }

  return EventBroker;
  
});
