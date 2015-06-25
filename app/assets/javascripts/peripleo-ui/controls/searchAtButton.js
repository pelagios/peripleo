define(['common/formatting', 'peripleo-ui/events/events'], function(Formatting, Events) {
  
  var SearchAtButton = function(container, eventBroker) {
            
    var element = jQuery(
          '<span>' +
          '  <span class="icon">&#xf041;</span>' +
          '  <span class="label"></span>' +
          '</span>'),
          
        /** DOM element shorthand **/
        label = element.find('.label'),
        
        /** Visibility flag, to avoid unnecessary DOM lookups **/
        isVisible = false,
        
        /** Current search phrase and result list **/
        currentQueryPhrase = false,
        currentResultList = [],
        
        /** Current selection and related totals **/
        selectedPlaces = false,
        relatedTotals = 0,
        
        /** Tests if the place is part of the current search result list **/
        isInCurrentResultList = function(place) {
          return currentResultList.some(function(result, idx) {
            return result.identifier === place.identifier;
          });
        },
        
        /** Updates the related result count through a one-time search **/
        updateRelatedCount = function() {
          var allCount = label.find('.all-count'),
              withQueryCount = label.find('.with-query-count'),
              hasQuery = (withQueryCount.length > 0),
              suffix = (hasQuery) ? '' : ' results';
              
          // We always need total related results
          eventBroker.fireEvent(Events.ONE_TIME_SEARCH, { 
            places: jQuery.map(selectedPlaces, function(p) { return p.identifier }),               
            query: false,
            callback: function(response) {
              count = response.total;
              allCount.html('(' + Formatting.formatNumber(count) + suffix + ')');  
            }
          });
          
          // If there's a query, we also need total results with query
          if (hasQuery) {
            eventBroker.fireEvent(Events.ONE_TIME_SEARCH, { 
              places: jQuery.map(selectedPlaces, function(p) { return p.identifier }),               
              query: currentQueryPhrase,
              callback: function(response) {
                count = response.total;
                withQueryCount.html('(' + Formatting.formatNumber(count) + ')');  
              }
            });
          }
        },
    
        /** Shows the element (and updates the label through a one-time search) **/
        show = function(selection) {
          selectedPlaces = jQuery.grep(selection, function(obj) {
            return obj.object_type === 'Place';
          });
          
          if (selectedPlaces.length > 0) {
            
            
            
            // TODO support multiple selected places, not just one
            
            
            
            // Explanation: if the selected place is part of the search results list, it is because
            // the user has searched for the place by name, e.g. "Roma". If we go into a subsearch now,
            // we'll want to ignore the query phrase! (It was for finding the *place*, not something
            // *linked* to it.) If the selected place is not in the search result list, it is
            // because the user has searched for some other term (e.g. "Inscription"), and the place
            // appared as "top place" on the map. In this case, we want to keep the query phrase!
            if (isInCurrentResultList(selectedPlaces[0]) || !currentQueryPhrase) {
              label.addClass('no-query all');
              label.html('Search at <span class="underline">' + Formatting.shortenPlacename(selectedPlaces[0].title) + 
                '</span> <span class="totals all-count"></span>');
            } else {
              label.removeClass('no-query all');
              label.html('Search  at ' + Formatting.shortenPlacename(selectedPlaces[0].title)  + ': <em class="query underline">' + 
                currentQueryPhrase + '</em>  <span class="totals with-query-count"></span> · <span class="underline all">all</span>  ' +
                '<span class="totals all-count"></span>');
            }

            updateRelatedCount();
            isVisible = true;
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
        },
        
        /** Handles 'search at' click **/
        onClick = function(query) {
          var clearQuery = (currentQueryPhrase) ? query === false : false;
          
          hide();
          eventBroker.fireEvent(Events.TO_STATE_SUB_SEARCH, { 
            places: selectedPlaces, 
            clear_query: clearQuery,
            total: relatedTotals 
          }); 
          
          currentQueryPhrase = query;
        };

    container.hide();
    container.append(element);
    
    element.on('click', '.all', function() { onClick(false); });
    element.on('click', '.query', function() { onClick(currentQueryPhrase); });
    
    eventBroker.addHandler(Events.SELECTION, onSelect);
    eventBroker.addHandler(Events.API_SEARCH_RESPONSE, function(response) { 
      currentQueryPhrase = response.params.query;
      currentResultList = response.items;
      if (isVisible)
        updateRelatedCount(); 
    });
    eventBroker.addHandler(Events.SEARCH_CHANGED, function(diff) {
      if (diff.hasOwnProperty('query'))
        currentQueryPhrase = diff.query;
    });
  };
  
  return SearchAtButton;
  
});
