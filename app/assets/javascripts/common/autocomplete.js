define(function() {
  
  var AutoComplete = function(form, input) {
    input.typeahead({
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
        
    input.on('typeahead:selected', function(e) {
      form.submit();
    });
  };
  
  return AutoComplete;
  
});
