define(['common/formatting',
        'peripleo-ui/controls/selection/selectionInfo',
        'peripleo-ui/events/events'], function(Formatting, SelectionInfo, Events) {
    
  var SLIDE_DURATION = 180;
  
  var SelectedPlace = function(container, eventBroker) {
    
    var self = this,
    
        content = jQuery(
          '<div class="content">' +
          '  <h3></h3>' +
          '  <p>' +
          '    <span class="temp-bounds"></span>' +
          '    <span class="top-places"></span>' +
          '  </p>' +
          '  <p class="names"></p>' +
          '  <p class="description"></p>' +
          '  <ul class="uris"></ul>' +
          '</div>'),
          
        /** DOM element shorthands **/
        heading = content.find('h3'),
        tempBounds = content.find('.temp-bounds'),
        names = content.find('.names'),
        description = content.find('.description'),
        uris = content.find('.uris'),
        
        /** Clears the contents **/
        clearContent = function() {
          heading.empty();
          tempBounds.empty();
          tempBounds.hide();
          names.empty();
          description.empty();
          uris.empty();
        },
        
        /** Fills the content **/
        fill = function(obj) {   
          heading.html(obj.title);
          
          if (obj.temporal_bounds) {
            if (obj.temporal_bounds.start === obj.temporal_bounds.end)
              tempBounds.html(Formatting.formatYear(obj.temporal_bounds.start));
            else 
              tempBounds.html(Formatting.formatYear(obj.temporal_bounds.start) + ' - ' + Formatting.formatYear(obj.temporal_bounds.end));
            tempBounds.show();
          }
          
          if (obj.names)
            names.html(obj.names.slice(0, 8).join(', '));
          
          if (obj.description)
            description.html(obj.description);
          
          
          uris.append(jQuery('<li>' + Formatting.formatGazetteerURI(obj.identifier) + '</li>'));
          if (obj.matches)
            jQuery.each(obj.matches, function(idx, uri) {
              uris.append(jQuery('<li>' + Formatting.formatGazetteerURI(uri) + '</li>'));
            });
        };

    container.append(content);
    SelectionInfo.apply(this, [ container, eventBroker, fill, clearContent ]);
    
    eventBroker.addHandler(Events.SELECT_MARKER, self.show);
    eventBroker.addHandler(Events.SELECT_RESULT, self.show);
  };
  
  SelectedPlace.prototype = Object.create(SelectionInfo.prototype);
  
  return SelectedPlace;
  
});
