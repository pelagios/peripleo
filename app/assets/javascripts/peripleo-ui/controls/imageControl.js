define(['peripleo-ui/events/events'], function(Events) {
  
  var RECT_THUMB_SIZE = 120;

  /**
   * NOTES...
   * 
   * first we need to change the structure of depiction info, in RDF as well as in JSON
   * (cross-check with current Wiki spec):
   * 
   * ---
   * 
   *    foaf:depiction <http://example.com/images/image.jpg> ;
   *    .
   * 
   * <http://example.com/images/image.jpg> a foaf:Image ;
   *    foaf:thumbnail <http://example.com/images/thumbs/thumb.jpg> ;
   *    dcterms:license <http:/...> ;
   *    dcterms:creator "Rainer" ;
   * 
   * OR
   * 
   *   foaf:thumbnail <http://example.com/thumbs/thumbs.jpg> ; # If thumb ONLY
   * 
   * ---  
   * 
   * depictions: [
   *   { "image_src" : "http://example.com/images/image.jpg",
   *     "thumb_src" : "http://example.com/thumbs/thumb.jpg",
   *     "creator" : "Rainer",
   *     "license" : "http://..." }
   * ]
   * 
   * Either image_src OR thumb_src must be specified, but not both.
   * 
   */

  var ImageControl = function(container, eventBroker) {

    var padding = parseInt(container.css('padding-left')),
    
        workArea = jQuery(
          '<div id="image-workshop">' + 
          '</div>'),
          
        /** Loads an image into the work area **/
        loadToWorkArea = function(src, callback) {
          var img = new Image();
          img.onload = function() { callback(img); };
          img.src = src;
          workArea.append(jQuery(img));
          return img;
        },
        
        /** Clips and scales the image to a rectangle using CSS **/
        clipToRect = function(image) {
          var img = jQuery(image),
              w = image.width, 
              h = image.height,
              scaling, offset, clipCSS;
            
          // Common CSS styles
          img.css('position', 'absolute');
          
          if (w > h) { // Landscape
            scaling = RECT_THUMB_SIZE / h;
            offset = (w - h) / 2 * scaling;
            img.css({ 
              left: - offset,
              height: RECT_THUMB_SIZE,
              clip: 'rect(0 ' + (offset + RECT_THUMB_SIZE) + 'px ' + RECT_THUMB_SIZE + 'px ' + offset + 'px)'
            });
          } else { // Portrait
            scaling = RECT_THUMB_SIZE / w;
            offset = (h - w) / 2 * scaling;
            img.css({ 
              left: 0,
              top: - offset,
              width: RECT_THUMB_SIZE,
              clip: 'rect(' + offset + 'px ' + RECT_THUMB_SIZE + 'px ' + (offset + RECT_THUMB_SIZE) + 'px 0)'
            });
          }        
          
          addThumbnail(img);
        },
        
        /** Moves the image from the hidden work area to the UI component **/
        addThumbnail = function(image) {
          var index = container.children().length,
              offset = padding + (padding + RECT_THUMB_SIZE) * index,
              left = parseInt(image.css('left')) + offset;
          
          image.css('left', left);
          container.append(image);
          image.show();
        },
        
        update = function(response) {
          var objectsWithDepictions = jQuery.grep(response.items, function(item) {
                return item.hasOwnProperty('depictions');
              }),
              
              allDepictions = jQuery.map(objectsWithDepictions, function(item) {
                return item.depictions;
              });
              
          workArea.empty();
          container.empty();
              
          if (allDepictions.length > 0) {
            jQuery.each(allDepictions.slice(0, 3), function(idx, src) {
              loadToWorkArea(src, clipToRect);
            });
          }
        };
    
    workArea.hide();
    container.append(workArea);

    eventBroker.addHandler(Events.API_VIEW_UPDATE, update);
  };
  
  return ImageControl;

});
