var util = util || {};

/** Loops through all elements with CSS class .number and formats using numeral.js **/
util.formatNumbers = function() {
  $.each($('.number'), function(idx, el) {
    var formatted = numeral($(el).text()).format('0,0');
    $(el).html(formatted);
  });
}

/** Renders an image icon corresponding to a specific license URL **/
util.licenseIcon = function(url) {
  if (url.indexOf('http://opendatacommons.org/licenses/odbl') == 0) {
    return '<a class="license" href="http://opendatacommons.org/licenses/odbl" target="_blank" title="Open Data Commons Open Database License (ODbL)"><img src="/api-v3/static/images/open-data-generic.png"></a>';
  } else if (url.indexOf('http://creativecommons.org/publicdomain/zero/1.0') == 0) {
	return '<a class="license" href="http://creativecommons.org/publicdomain/zero/1.0" target="_blank" title="CC0 Public Domain Dedication"><img src="/api-v3/static/images/cc-zero.png"></a>';	  
  }
  
  return '<a href="' + url + '">' + url + '</a>';
} 

