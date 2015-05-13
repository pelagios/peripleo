define(function() {
  
  /** Flag that indicates wether the device supports touch events **/
  var hasTouch = ('ontouchstart' in window) || (navigator.MaxTouchPoints > 0);
  
  return {
    
    makeXDraggable: function(element, onDrag, onStop, opt_containment) {
      element.draggable({ 
        axis: 'x', 
        containment: opt_containment,
        drag: onDrag,
        stop: onStop
      });      
    }
    
  };
  
  /*
   * TODO mimic jQuery's draggable method:
   * 
   * We need to constrain to X axis, provide onDrag/onStop callbacks and - possibly - a parent containment
   *  
   *
          element.draggable({ 
            axis: 'x', 
            containment: opt_containment,
            drag: onDrag,
            stop: onStop
          });
   */
  
  /*
            element.bind('touchmove', function(e) {
              console.log(e);
            });
   */

});
