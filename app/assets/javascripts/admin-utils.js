var util = util || {};

/** Enables the file upload buttons **/
util.enableUploads = function() {
  // Wire CSS-styled upload button to hidden file input 
  $('.upload').click(function(e) {
    var inputId = e.target.getAttribute('data-input');
    $('#' + inputId).click();  
  });

  // Set the hidden inputs to auto-submit
  $('input:file').change(function(e) {
    e.target.parentNode.submit();  
  });
}