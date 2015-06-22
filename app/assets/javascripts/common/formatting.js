define(function() {
  
  var METER_TEMPLATE = 
    '<li>' +
    '  <div class="meter"><div class="bar"></div><div class="label"></div></div>' +
    '</li>';
  
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
    
    /** Formats a single number on demand **/
    formatNumber: function(n) {
      return numeral(n).format('0,0');
    },
    
    /** Helper to format an integer year for screen display **/
    formatYear: function(year) { 
      if (year < 0) return -year + ' BC'; else return year + ' AD'; 
    },
    
    /** Creates a 'shortcode label' from a gazetteer URI **/
    formatGazetteerURI: function(uri) {
      var prefix, gazId;

      if (uri.indexOf('http://pleiades.stoa.org/places/') === 0) {
        prefix = 'pleiades';
        gazId = uri.substr(32);
      } else if (uri.indexOf('http://atlantides.org/capgrids/') === 0) {
        prefix = 'atlantides';
        gazId = uri.substr(31);
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
    },
    
    /** Creates a nice & short representation for a source URL **/
    formatSourceURL: function(url) {
      var urlWithoutProtocol = (url.indexOf('http') === 0) ? url.substring(url.indexOf(':') + 3) : url;
      if (urlWithoutProtocol.indexOf('/') > 0) 
        return '<a target="_blank" href="' + url + '">' + urlWithoutProtocol.substring(0, urlWithoutProtocol.indexOf('/')) + '</a>';
      else
        return '<a target="_blank" href="' + url + '">' + urlWithoutProtocol + '</a>';
    },
    
    formatFacetLabel: function(label) {
      if (label.indexOf('gazetteer:') === 0) {
        // Gazetteer label
        return label.substring(10);
      } else if (label.indexOf('#') > -1) {
        // Dataset label
        return label.substring(0, label.indexOf('#'));
      } else {
        return label;
      }
    },
    
    createMeter: function(label, tooltip, percentage) {
      var row = jQuery(METER_TEMPLATE),
          bar = row.find('.bar');
            
      bar.css('width', percentage + '%');
      bar.attr('title', tooltip);
      row.find('.label').html(label);
      
      return row;
    }
    
  };
  
  return Formatting;
  
});
