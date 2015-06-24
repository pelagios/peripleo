define(['peripleo-ui/events/events'], function(Events) {

      /** Thumbnail height **/  
  var THUMB_HEIGHT = 98,
  
      /** Thumbnail widths in 1, 2 or 3-thumbnail layout **/
      THUMB_WIDTH = {
        1: 396,
        2: 197,
        3: 131 
      }
      
      /** Panel open/close slide duration **/
      SLIDE_DURATION = 180,
      
      /** Crossfade duration **/
      FADE_DURATION = 300,
      
      /** A minimum time the image control stays idle after the last refresh **/
      QUERY_DELAY_MS = 500;
  
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

        /** Container element holding the thumbnail images **/
    var thumbnailContainer = jQuery('<div id="thumbnail-container"></div>'),
    
        /** 'Control caption' overlaid on top of the images **/
        caption = jQuery('<span class="caption">Images</span>'),
    
        /** Off-screen staging area where image loading and preparation takes place **/
        workArea = jQuery('<div id="image-workshop"></div>'),
        
        /** Flag indicating whether images are currently loading **/
        busy = false,
        
        /** A buffered update to perform as soon as busy = false **/
        pendingUpdate = false,
        
        /** To keep track of currently displayed images **/
        currentImages = {},
        
        /** Flag to buffer current visibility **/
        isHidden = true,
        
        show = function() {
          isHidden = false;
          container.velocity('slideDown', { duration: SLIDE_DURATION });
        },
        
        hide = function() {
          isHidden = true;
          container.velocity('slideUp', { duration: SLIDE_DURATION });
        },
          
        /** Loads an image into the work area **/
        loadToWorkArea = function(src, onSuccess, onError) {
          var img = new Image();
          img.onload = function() { onSuccess(img); };
          img.onerror = function() { 
            jQuery(img).remove();
            onError();
          };
          img.src = src;
          workArea.append(jQuery(img));
          return img;
        },
        
        clip = function(image, targetWidth, targetHeight) {
          var w = image.width,
              h = image.height,
              imageAspectRatio = w / h,
              targetAspectRatio = targetWidth / targetHeight,
              jImage = jQuery(image), // Wrapped for easer CSS manipulation
              scale, offset;
              
          jImage.css({ position: 'absolute', display: 'none' });
          
          if (imageAspectRatio > targetAspectRatio) { // Clip left and right
            scale = targetHeight / h;
            offset = (w - h * targetAspectRatio) / 2 * scale;
            jImage.css({
              top:0,
              left: Math.round(- offset),
              height: targetHeight,
              clip: 'rect(0 ' + (offset + targetWidth) + 'px ' + targetHeight + 'px ' + offset + 'px)'              
            });
          } else { // Clip top and bottom
            scale = targetWidth / w;
            offset = (h - w / targetAspectRatio) / 2 * scale;
            jImage.css({
              top: Math.round(- offset),
              left:0,
              width: targetWidth,
              clip: 'rect(' + offset + 'px ' + targetWidth + 'px ' + (offset + targetHeight) + 'px 0)'              
            });
          }
          
          return jImage;
        },

        insertIfNotExists = function(obj, offset, width, onComplete) {
          var existing = currentImages[obj.src],
              alreadyShown = existing && existing.offset === offset && existing.width === width;
          
          if (alreadyShown) {
            onComplete();
            return existing.img;
          } else {
            loadToWorkArea(obj.src, function(image) {
              var clippedImage = clip(image, width, THUMB_HEIGHT);
              
              currentImages[obj.src] = { offset: offset, width: width, img: clippedImage };
            
              clippedImage.css('left', parseInt(clippedImage.css('left')) + offset);
              thumbnailContainer.append(clippedImage);
              clippedImage.velocity('fadeIn', { duration: FADE_DURATION });
              
              if (onComplete)
                onComplete();
            }, onComplete);
            return false;
          }
        },
        
        remove = function(images) {
          jQuery.each(images, function(idx, img) {
            delete currentImages[img.src];
          });
          images.velocity('fadeOut', { duration: FADE_DURATION, complete: function() { images.remove(); }});
        },
        
        handlePending = function() {
          setTimeout(function() {
            busy = false;
            if (pendingUpdate) {
              update(pendingUpdate);                    
              pendingUpdate = false;
            }
          }, QUERY_DELAY_MS);
        },
        
        /** Updates the image widget **/
        update = function(objects) {          
          var imagesToRemove = thumbnailContainer.children(),
          
              objectsWithDepictions = jQuery.grep(objects, function(obj) {
                return obj.hasOwnProperty('depictions');
              }),
              
              // A 'flat map' generating an array of (depiction -> object) tuples
              depictions = jQuery.map(objectsWithDepictions, function(obj) {
                return jQuery.map(obj.depictions, function(src) {
                  return { src: src, obj: obj };
                });
              }),
              
              // TODO pick random 3 rather than top 3?
              toShow = depictions.slice(0, 3),
              
              width = THUMB_WIDTH[toShow.length],
              
              // To track progress
              imagesLoadedCtr = 0;
          
          if (toShow.length > 0) {
            if (isHidden)
              show();
              
            busy = true;
            
            jQuery.each(depictions.slice(0, 3), function(idx, obj) {
              var existingImage = insertIfNotExists(obj, (width + 2) * idx, width, function() {
                imagesLoadedCtr++;
                
                if (imagesLoadedCtr == toShow.length)
                  handlePending();
              });
              
              if (existingImage)
                imagesToRemove = imagesToRemove.not(existingImage);
            });
          } else {
            hide();
          }
           
          remove(imagesToRemove);
        };

    container.hide();
    container.append(thumbnailContainer);
    container.append(caption);

    eventBroker.addHandler(Events.API_VIEW_UPDATE, function(response) {
      var objects = response.top_places.concat(response.items);
      if (busy) {
        pendingUpdate = objects;
      } else {
        update(objects);
        pendingUpdate = false;
      }
    });
  };
  
  return ImageControl;

});
