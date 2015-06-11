define(['peripleo-ui/events/events', 'common/formatting'], function(Events, Formatting) {
  
  var SearchAtButton = function(container, eventBroker) {
            
    var element = jQuery(
          '<a href="#">' +
          '  <span class="icon">&#xf041;</span>' +
          '  <span class="label">Search <em class="placename"></em></span>' +
          '  <span class="totals"></span>' +
          '</a>'),
        
        /** DOM element shorthand **/
        placeName = element.find('.placename'),
        totals = element.find('.totals'),
        
        /** Current selection **/
        selectedPlaces = false,
        
        /** Updates the related result count HTML field **/
        updateRelatedCount = function(response) {
          totals.html('(' + Formatting.formatNumber(response.total) + ' results)');  
        },
    
        /** Updates the label and shows the element **/
        show = function(selection) {
          selectedPlaces = jQuery.grep(selection, function(obj) {
            return obj.object_type === 'Place';
          });
          
          if (selectedPlaces.length > 0) {
            // TODO support multiple selected places, not just one
            placeName.html(selectedPlaces[0].title);
    
            // Fetch related result count via API
            eventBroker.fireEvent(Events.ONE_TIME_SEARCH,
              {  place: selectedPlaces[0].identifier, callback: updateRelatedCount });
           
            // TODO what if the filter settings change? -> count should update
                        
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
