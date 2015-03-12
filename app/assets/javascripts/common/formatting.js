define(function() {
  
  var Formatting = {
    
    /** Runs numeral.js over all elements with CSS class 'number'.
      * 
      * If no DOM element is provided, the function will run over the entire page.
      */
    formatNumbers: function(opt_parent) {
      var elements = (opt_parent) ? $(opt_parent).find('.number') : $('.number');	
      $.each(elements, function(idx, el) {
        var formatted = numeral($(el).text()).format('0,0');
        $(el).html(formatted);
      });
    }
    
  }
  
  return Formatting;
  
});
