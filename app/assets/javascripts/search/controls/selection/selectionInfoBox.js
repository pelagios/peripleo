define(['search/events', 'common/formatting'], function(Events, Formatting) {
  
  var SelectionInfoBox = function(container, eventBroker) {
    
    var SLIDE_DURATION = 100;
    
    var element = jQuery(
          '<div id="selection-info">' +
          '  <div class="content">' +
          '    <h3></h3>' +
          '    <p>' +
          '      <span class="temp-bounds"></span>' +
          '      <span class="top-places"></span>' +
          '    </p>' +
          '    <p class="names"></p>' +
          '    <p class="description"></p>' +
          '    <ul class="uris"></ul>' +
          '    <p class="homepage"></p>' +
          '    <p class="related"></p>' +
          '  </div>' +
          '  <div class="thumbnail">' +
          '  </div>' +
          '</div>'),

        /** The currently displayed object **/
        currentObject = false,
          
        /** DOM elements **/
        content = element.find('.content'),
        thumbnail = element.find('.thumbnail'),
          
        heading = element.find('h3'),
        tempBounds = element.find('.temp-bounds'),
        topPlaces = element.find('.top-places'),
        names = element.find('.names'),
        description = element.find('.description'),
        uris = element.find('.uris'),
        homepage = element.find('.homepage'),
        related = element.find('.related'),
        
        ignoreQueryPhrase = false,
        
        clearTemplate = function() {
          // Empty all container elements
          thumbnail.empty();

          heading.empty();
          tempBounds.empty();
          topPlaces.empty();
          names.empty();
          description.empty();
          uris.empty();
          homepage.empty();
          related.empty();

          // In addition, hide homepage (so we don't see the :before icons)...
          homepage.hide();

          // ... and set the content to 'wide mode' (without thumbnail)
          content.removeClass('with-thumb');
        },
        
        fillTemplate = function(obj) {   
          var img;

          heading.html(obj.title);
          
          if (obj.temporal_bounds) {
            tempBounds.show();
            if (obj.temporal_bounds.start === obj.temporal_bounds.end)
              tempBounds.html(Formatting.formatYear(obj.temporal_bounds.start));
            else 
              tempBounds.html(Formatting.formatYear(obj.temporal_bounds.start) + ' - ' + Formatting.formatYear(obj.temporal_bounds.end));
          } else {
            tempBounds.hide();
          }
                
          if (obj.top_places) {
            topPlaces.html('<span class="icon">&#xf041;</span><span class="top">' + obj.top_places[0].title + '</span>');
            topPlaces.click(function() {
              eventBroker.fireEvent(Events.SELECT_RESULT, obj.top_places[0]);
            });
          }
          
          if (obj.names)
            names.html(obj.names.slice(0, 8).join(', '));
          
          if (obj.description)
            description.html(obj.description);
          
          if (obj.object_type === 'Place') {
            uris.append(jQuery('<li>' + Formatting.formatGazetteerURI(obj.identifier) + '</li>'));
            if (obj.matches) {
              jQuery.each(obj.matches, function(idx, uri) {
                uris.append(jQuery('<li>' + Formatting.formatGazetteerURI(uri) + '</li>'));
              });
            }
          } else if (obj.homepage) {
            homepage.append(Formatting.formatSourceURL(obj.homepage));
            homepage.show();
          }
            
          // TODO pick random rather than always first?
          if (obj.depictions && obj.depictions.length > 0) {
            content.addClass('with-thumb');
            
            img = jQuery('<img src="' + obj.depictions[0] + '">');
            img.error(function() { 
              // If the image fails to load, just create a DIV we can style via CSS
              thumbnail.html('<div class="img-404"></div>');
            });
            thumbnail.append(img);
          }
          
          if (obj.result_count) {
            ignoreQueryPhrase = false;
            related.html(Formatting.formatNumber(obj.result_count) + ' related results');
            eventBroker.fireEvent(Events.CONTROLS_ANIMATION_END);
          } else if (obj.object_type === 'Place') {
            // A place was selected that came as a search result, not a facet
            // In this case we ignore the query phrase, since it was used to find the place, not to filter the search further
            ignoreQueryPhrase = true;
            
            eventBroker.fireEvent(Events.ONE_TIME_SEARCH,
              { 
                place: obj.identifier, query: false,
                callback: function(response) { 
                  related.html(Formatting.formatNumber(response.total) + ' related results');  
                  eventBroker.fireEvent(Events.CONTROLS_ANIMATION_END);
                }
              });
          }
          
        },
        
        /** Fetches additional info about the places referenced by the item **/
        fetchItemExtras = function(item, callback) {
          jQuery.getJSON('/api-v3/items/' + item.identifier + '/places', function(response) {
            item.num_unique_places = response.total;
            item.top_places = response.items;
            
            // Just make sure no other object was selected in the meantime
            if (currentObject.identifier === item.identifier)
              callback(item);            
          });
        },
        
        /**
         * Fetches additional object details via the API, merges the response with
         * the original object, and passes the result back to the callback
         * function.
         */
        fetchExtras = function(obj, callback) {
          var id = obj.identifier;
          
          if (obj.object_type === 'Place') {
            // TODO implement
            callback(obj);
          } else if (obj.object_type === 'Item') {
            fetchItemExtras(obj, callback);
          } else if (obj.object_type === 'Dataset') {
            // TODO implement
            callback(obj);
          } else {
            console.log('Error: unkown object type "' + obj.object_type + '"');
          }
        },

        show = function(objects) {                
          // TODO support display of lists of objects, rather than just single one
          var obj = (jQuery.isArray(objects)) ? objects[0] : objects,
              currentType = (currentObject) ? currentObject.object_type : false;
          
          if (currentObject) { // Box is currently open    
            if (!obj) { // Close it
              element.slideToggle({ 
                duration: SLIDE_DURATION, 
                step: function() { eventBroker.fireEvent(Events.CONTROLS_ANIMATION); },
                complete: function() {
                  currentObject = false;
                  clearTemplate();
                  eventBroker.fireEvent(Events.CONTROLS_ANIMATION_END);
                  eventBroker.fireEvent(Events.SELECTION); // Deselect event      
                }
              });
            } else {
              if (currentObject.identifier !== obj.identifier) { // New object - reset
                currentObject = obj;
                clearTemplate();
                fetchExtras(obj, fillTemplate);
                eventBroker.fireEvent(Events.CONTROLS_ANIMATION);
                eventBroker.fireEvent(Events.SELECTION, obj); 
              }
            }
          } else { // Currently closed 
            if (obj) { // Open
              currentObject = obj;
              element.slideToggle({ 
                duration: SLIDE_DURATION,
                step: function() { eventBroker.fireEvent(Events.CONTROLS_ANIMATION); },
                complete: function() { eventBroker.fireEvent(Events.CONTROLS_ANIMATION_END); }
              });
              fetchExtras(obj, fillTemplate);
              eventBroker.fireEvent(Events.SELECTION, obj); 
            }
          }  
        },
        
        hide = function() {
          currentObject = false;
          clearTemplate();
          element.slideUp(SLIDE_DURATION);
        };
    
    element.on('click', '.related', function() {
      var type = (currentObject) ? currentObject.object_type : false,
          searchParams = { place: currentObject.identifier };
          
      if (ignoreQueryPhrase)
        searchParams.query = false;

      if (type === 'Place')      
        eventBroker.fireEvent(Events.SUB_SEARCH, searchParams); 
    });

    homepage.hide();
    element.hide();
    container.append(element);
    
    eventBroker.addHandler(Events.SELECT_MARKER, show);
    eventBroker.addHandler(Events.SELECT_RESULT, show);
    eventBroker.addHandler(Events.QUERY_PHRASE_CHANGED, function(query) {
      if (query) { // No need to hide if the user just cleared the search
        eventBroker.fireEvent(Events.SELECTION);
        hide(); 
      }
    });
    eventBroker.addHandler(Events.API_SEARCH_RESPONSE, function(response) {
      if (element.is(':visible') && currentObject && currentObject.object_type === 'Place') {
        eventBroker.fireEvent(Events.ONE_TIME_SEARCH,
          { 
            place: currentObject.identifier,
            callback: function(response) { 
              related.html(Formatting.formatNumber(response.total) + ' related results');  
            }
        });
      }
    });
  };
  
  return SelectionInfoBox;
  
});
