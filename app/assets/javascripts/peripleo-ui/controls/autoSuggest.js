define(function() {
  
  var AutoSuggest = function(form, input) {
    input.typeahead({
      hint: false,
      highlight: true,
      minLength: 1
    },{
      displayKey: 'val',
      source: function(query, callback) {
        jQuery.getJSON('/peripleo/autocomplete?q=' + query, function(results) {
          callback(results);
        });
      }
    });
        
    input.on('typeahead:selected', function(e) {
      form.submit();
    });
    

    this.clear = function() {
      input.typeahead('val','');
    };
  };
  
  return AutoSuggest;
  
});
