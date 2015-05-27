define([], function() {

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

  var ThumbnailWidget = function() {
    
    var element = jQuery(
          '<div>' +
          '</div>');
    
  };
  
  return ThumbnailWidget;

});
