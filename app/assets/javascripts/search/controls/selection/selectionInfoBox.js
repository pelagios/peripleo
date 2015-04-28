define(['search/events', 'common/formatting'], function(Events, Formatting) {
  
  var SelectionInfoBox = function(container, eventBroker) {
    
    var SLIDE_DURATION = 200;
    
    var element = jQuery(
          '<div id="selection-info">' +
          '  <div class="content">' +
          '    <h3></h3>' +
          '    <p class="names"></p>' +
          '    <p class="description"></p>' +
          '    <ul class="uris"></ul>' +
          '    <p class="related"></p>' +
          '  </div>' +
          '  <div class="thumbnail">' +
          '  </div>' +
          '</div>'),
          
        content = element.find('.content'),
        
        thumbnail = element.find('.thumbnail'),
          
        currentObject = false,
        
        title = element.find('h3'),
        
        names = element.find('.names'),
        
        description = element.find('.description'),
        
        uris = element.find('.uris'),
        
        related = element.find('.related'),
        
        ignoreQueryPhrase = false,
        
        fillTemplate = function(obj) {   
          var img;
           
          title.html(obj.title);
          
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
          }
          
          if (obj.result_count) {
            ignoreQueryPhrase = false;
            related.html(Formatting.formatNumber(obj.result_count) + ' related results');
          } else if (obj.object_type === 'Place') {
            // A place was selected that came as a search result, not a facet
            // In this case we ignore the query phrase, since it was used to find the place, not to filter the search further
            ignoreQueryPhrase = true;
            
            eventBroker.fireEvent(Events.ONE_TIME_SEARCH,
              { 
                place: obj.identifier, query: false,
                callback: function(response) { 
                  related.html(Formatting.formatNumber(response.total) + ' related results');  
                }
              });
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
        },
        
        clearTemplate = function() {
          title.empty();
          names.empty();
          description.empty();
          uris.empty();
          related.empty();
          
          content.removeClass('with-thumb');
          thumbnail.empty();
        },

        showObject = function(obj) {      
          var currentType = (currentObject) ? currentObject.object_type : false;
          
          if (currentObject) { // Box is currently open    
            if (!obj) { // Close it
              element.slideToggle(SLIDE_DURATION, function() {
                currentObject = false;
                clearTemplate();
                eventBroker.fireEvent(Events.SELECTION); // Deselect event      
                
                // If the user de-selected a place, place search filter is cleared automatically
                if (currentType === 'Place')
                  eventBroker.fireEvent(Events.SEARCH_CHANGED, { place : false });
              });
            } else {
              if (currentObject.identifier !== obj.identifier) { // New object - reset
                currentObject = obj;
                clearTemplate();
                fillTemplate(obj);
                eventBroker.fireEvent(Events.SELECTION, obj); 
                
                // If the user de-selected a place, place search filter is cleared automatically
                if (currentType === 'Place')
                  eventBroker.fireEvent(Events.SEARCH_CHANGED, { place : false });
              }
            }
          } else { // Currently closed 
            if (obj) { // Open
              currentObject = obj;
              element.slideToggle(SLIDE_DURATION);
              fillTemplate(obj);
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
    
    element.hide();
    container.append(element);
    
    eventBroker.addHandler(Events.SELECT_MARKER, showObject);
    eventBroker.addHandler(Events.SELECT_RESULT, showObject);
    eventBroker.addHandler(Events.QUERY_PHRASE_CHANGED, function(query) {
      if (query) { // No need to hide if the user just cleared the search
        eventBroker.fireEvent(Events.SELECTION);
        hide(); 
      }
    });
  };
  
  return SelectionInfoBox;
  
});
