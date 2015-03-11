require([], function() {

  jQuery(document).ready(function() {
    var form = jQuery('#search-form form'),
        searchField = jQuery('#query');
        
    /** Search auto-complete **/
    searchField.typeahead({
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
        
    searchField.on('typeahead:selected', function(e) {
      form.submit();
    });
        
    form.keypress(function (e) {
      if (e.which == 13)
        form.submit();
    });
  });  
  
});
  
