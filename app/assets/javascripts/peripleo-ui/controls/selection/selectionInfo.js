/** Common code for SelectedPlace and SelectedItem boxes **/
define(function() {
    
  var SLIDE_DURATION = 180;
  
  var SelectionInfo = function(container, eventBroker, fill, clearContent) {
    
    var currentObject = false,
    
        slideDown = function() {
          container.velocity('slideDown', { duration: SLIDE_DURATION });
        },
      
        slideUp = function(opt_complete) {
          container.velocity('slideUp', { 
            duration: SLIDE_DURATION,
            complete: function() { 
              clearContent();
              if (opt_complete)
                opt_complete();
            }
          });
        },
        
        hide = function()  {
          currentObject = false;
          slideUp();          
        },

        show = function(objects) {         
          // TODO support display of lists of objects, rather than just single one
          var obj = (jQuery.isArray(objects)) ? objects[0] : objects,
              currentType = (currentObject) ? currentObject.object_type : false;
          
          if (currentObject) { // Box is currently open    
            if (!obj) { // Close it
              hide();
            } else { 
              if (currentObject.identifier !== obj.identifier) { // New object - change
                currentObject = obj;
                slideUp(function() { 
                  fill(obj);
                  slideDown(); 
                });
              }
            }
          } else { // Currently closed 
            if (obj) { // Open it
              currentObject = obj;
              fill(obj);
              slideDown();
            }
          }  
        };
        
    this.show = show;
    this.hide = hide;
    container.hide();
  };
  
  return SelectionInfo;
  
});
