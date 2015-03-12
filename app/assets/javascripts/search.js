require(['common/formatting'], function(Formatting) {

  jQuery(document).ready(function() {
    var form = jQuery('#search form'),
        textInput = jQuery('#search input[type="text"]');
        
    /** Search auto-complete **/
    textInput.typeahead({
      hint: true,
      highlight: true,
      minLength: 1
    },{
      displayKey: 'key',
      source: function(query, callback) {
        jQuery.getJSON('/api-v3/new/autosuggest?q=' + query, function(results) {
          callback(results);
        });
      }
    });
        
    textInput.on('typeahead:selected', function(e) {
      form.submit();
    });
        
    form.keypress(function (e) {
      if (e.which == 13)
        form.submit();
    });
    
    /** Format numbers **/
    Formatting.formatNumbers();
  });  
  
});
  
