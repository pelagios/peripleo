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
        
        /** Updates the related result count through a one-time search **/
        updateRelatedCount = function() {
          eventBroker.fireEvent(Events.ONE_TIME_SEARCH,
            {  
              place: selectedPlaces[0].identifier,
              callback: function(response) {
                totals.html('(' + Formatting.formatNumber(response.total) + ' results)');  
              }
            }
          );
        },
    
        /** Shows the element (and updates the label through a one-time search) **/
        show = function(selection) {
          selectedPlaces = jQuery.grep(selection, function(obj) {
            return obj.object_type === 'Place';
          });
          
          if (selectedPlaces.length > 0) {
            // TODO support multiple selected places, not just one
            placeName.html(selectedPlaces[0].title);   

            updateRelatedCount();
           
            // TODO what if the filter settings change? -> count should update
                        
            container.show();
          } else {
            selectedPlaces = false;
            hide();
          }
        },
        
        /** Hides the element **/
        hide = function() {
          container.hide();
        },
        
        /** 
         * Handles the 'select' event. 
         * 
         * After this event, the element should either be shown or hidden,
         * depending on whether the user selected a place (show), or deselected
         * or selected an object (hide).
         */
        onSelect = function(selection) {
          if (selection)
            show(selection);
          else
            hide();
        };

    container.hide();
    container.append(element);
    
    element.click(function() {
      hide();
      eventBroker.fireEvent(Events.SUB_SEARCH, selectedPlaces); 
      return false;
    });
    
    eventBroker.addHandler(Events.SELECTION, onSelect);
    eventBroker.addHandler(Events.API_SEARCH_RESPONSE, updateRelatedCount);
  };
  
  return SearchAtButton;
  
});
