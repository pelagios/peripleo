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
    },
    
    formatGazetteerURI: function(uri) {
      var prefix, gazId;
          
      if (uri.indexOf('http://pleiades.stoa.org/places/') === 0) {
        prefix = 'pleiades';
        gazId = uri.substr(32);
      } else if (uri.indexOf('http://dare.ht.lu.se/places/') === 0) {
        prefix = 'dare';
        gazId = uri.substr(28);
      } else if (uri.indexOf('http://gazetteer.dainst.org/place/') === 0) {
        prefix = 'idai';
        gazId = uri.substr(34);
      } else if (uri.indexOf('http://vici.org/vici/') === 0) {
        prefix = 'vici';
        gazId = uri.substr(21);
      } else if (uri.indexOf('http://chgis.hmdc.harvard.edu/placename/') === 0) {
        prefix = 'chgis';
        gazId = uri.substr(44);
      } else {
        // Bit of a hack...
        prefix = 'http';
        gazId = uri.substr(5);
      }
 
      return '<a class="gazetteer-uri ' + prefix + '" target="_blank" title="' + uri + '" href="' + uri + '">' + prefix + ':' + gazId + '</a>'; 
    }
    
  }
  
  return Formatting;
  
});
