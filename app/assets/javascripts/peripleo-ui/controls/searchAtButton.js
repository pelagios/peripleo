define(['peripleo-ui/events/events'], function(Events) {
  
  var SearchAtButton = function(container, eventBroker) {
            
    var element = jQuery(
          '<a href="#">' +
          '  <span class="icon">&#xf041;</span>' +
          '  <span class="label">Search at <em class="placename"></em></span>' +
          '</a>'),
        
        /** DOM element shorthand **/
        placeName = element.find('.placename'),
        
        /** Current selection **/
        selectedPlaces = false,
    
        /** Updates the label and shows the element **/
        show = function(selection) {
          selectedPlaces = jQuery.grep(selection, function(obj) {
            return obj.object_type === 'Place';
          });
          
          if (selectedPlaces.length > 0) {
            // TODO support multiple selected places, not just one
            placeName.html(selectedPlaces[0].title);
            container.show();
          } else {
            selectedPlaces = false;
            hide();
          }
        },
        
        hide = function() {
          container.hide();
        },
        
        onSelect = function(selection) {
          if (selection)
            show(selection);
          else
            hide();
        };

    container.hide();
    container.append(element);
    
    eventBroker.addHandler(Events.SELECT_MARKER, onSelect);
    eventBroker.addHandler(Events.SELECT_RESULT, onSelect);
    
    element.click(function() {
      hide();
      eventBroker.fireEvent(Events.SUB_SEARCH, selectedPlaces); 
      return false;
    });
  };
  
  return SearchAtButton;
  
});
