define(['peripleo-ui/events/events', 'common/formatting'], function(Events, Formatting) {
  
  var SearchAtButton = function(container, eventBroker) {
            
    var element = jQuery(
          '<a href="#">' +
          '  <span class="icon">&#xf041;</span>' +
          '  <span class="label">Search <em class="placename"></em></span>' +
          '  <span class="totals"></span>' +
          '</a>'),
        
        /** DOM element shorthand **/
        placeSpan = element.find('.placename'),
        totalsSpan = element.find('.totals'),
        
        /** Visibility flag, to avoid unnecessary DOM lookups **/
        isVisible = false,
        
        /** Current selection and related totals **/
        selectedPlaces = false,
        relatedTotals = 0,
        
        /** Updates the related result count through a one-time search **/
        updateRelatedCount = function() {
          eventBroker.fireEvent(Events.ONE_TIME_SEARCH,
            {  
              places: jQuery.map(selectedPlaces, function(p) { return p.identifier }), 
              callback: function(response) {
                relatedTotals = response.total;
                totalsSpan.html('(' + Formatting.formatNumber(relatedTotals) + ' results)');  
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
            placeSpan.html(selectedPlaces[0].title);   

            isVisible = true;
            updateRelatedCount();
            container.show();
          } else {
            selectedPlaces = false;
            hide();
          }
        },
        
        /** Hides the element **/
        hide = function() {
          isVisible = false;
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
      eventBroker.fireEvent(Events.TO_STATE_SUB_SEARCH, { places: selectedPlaces, total: relatedTotals }); 
      return false;
    });
    
    eventBroker.addHandler(Events.SELECTION, onSelect);
    eventBroker.addHandler(Events.API_SEARCH_RESPONSE, function() { 
      if (isVisible)
        updateRelatedCount(); 
    });
  };
  
  return SearchAtButton;
  
});
