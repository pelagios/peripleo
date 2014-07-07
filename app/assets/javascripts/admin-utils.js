var util = util || {};

/** Enables the file upload buttons **/
util.enableUploads = function() {
  // Wire CSS-styled upload button to hidden file input 
  $(document).on('click', '.upload', function(e) {
    var inputId = e.target.getAttribute('data-input');
    $('#' + inputId).click();  
  });

  // Set the hidden inputs to auto-submit
  $(document).on('change', 'input:file', function(e) {
    e.target.parentNode.submit();  
  });
}