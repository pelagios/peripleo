define(['common/formatting',
        'peripleo-ui/controls/selection/selectionInfo',
        'peripleo-ui/events/events'], function(Formatting, SelectionInfo, Events) {
    
  var SLIDE_DURATION = 180;
  
  var SelectedItem = function(container, eventBroker) {
    var self = this,
    
        content = jQuery(
          '<div class="content">' +
          '  <h3></h3>' +
          '  <p>' +
          '    <span class="temp-bounds"></span>' +
          '    <span class="top-places"></span>' +
          '  </p>' +
          '  <p class="description"></p>' +
          '  <p class="homepage"></p>' +
          '</div>'),
          
        /** DOM element shorthands **/
        heading = content.find('h3'),
        tempBounds = content.find('.temp-bounds'),
        names = content.find('.names'),
        description = content.find('.description'),
        homepage = content.find('.homepage'),
        
        /** Clears the contents **/
        clearContent = function() {
          heading.empty();
          tempBounds.empty();
          tempBounds.hide();
          names.empty();
          description.empty();
          homepage.empty();
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
          
          if (obj.description)
            description.html(obj.description);
          
          if (obj.homepage)
            homepage.append(Formatting.formatSourceURL(obj.homepage));
        };

    container.append(content);
    SelectionInfo.apply(this, [ container, eventBroker, fill, clearContent ]);
    
    eventBroker.addHandler(Events.SELECTION, function(results) {
      var firstResultType = (results) ? results[0].object_type : false;
      if (firstResultType === 'Item')
        self.show(results[0]);
      else
        self.hide();
    });    
  };
  
  SelectedItem.prototype = Object.create(SelectionInfo.prototype);
  
  return SelectedItem;
  
});

