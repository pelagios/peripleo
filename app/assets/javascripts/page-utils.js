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
    return '<a class="license" href="http://opendatacommons.org/licenses/odbl" title="Open Data Commons Open Database License (ODbL)"><img src="/api-v3/static/images/open-data-generic.png"></a>';
  }
  
  return '<a href="' + url + '">' + url + '</a>';
} 

