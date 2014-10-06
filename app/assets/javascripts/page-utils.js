var util = util || {};

/** Loops through all elements with CSS class .number and formats using numeral.js **/
util.formatNumbers = function(opt_parent) {
  var elements = (opt_parent) ? $(opt_parent).find('.number') : $('.number');	
  $.each(elements, function(idx, el) {
    var formatted = numeral($(el).text()).format('0,0');
    $(el).html(formatted);
  });
}

/** Renders an image icon corresponding to a specific license URL **/
util.licenseIcon = function(url) {
  if (url.indexOf('http://opendatacommons.org/licenses/odbl') == 0) {
    return '<a class="license" href="' + url + '" target="_blank" title="Open Data Commons Open Database License (ODbL)"><img src="/api-v3/static/images/open-data-generic.png"></a>';
  } else if (url.indexOf('http://creativecommons.org/publicdomain/zero/1.0') == 0) {
	return '<a class="license" href="' + url + '" target="_blank" title="CC0 Public Domain Dedication"><img src="/api-v3/static/images/cc-zero.png"></a>';	  
  } else if (url.indexOf('http://creativecommons.org/licenses/by-nc') == 0) {
	return '<a class="license" href="' + url + '" target="_blank" title="CC Attribution Non-Commercial (CC BY-NC)"><img src="/api-v3/static/images/cc-by-nc.png"></a>';	  
  } else {
    return '<a href="' + url + '">' + url + '</a>';
  }
} 

util.formatGazetteerURI = function(uri) {
  if (uri.indexOf('http://pleiades.stoa.org/places/') > -1) {
    return 'pleiades:' + uri.substr(32);
  } else if (uri.indexOf('http://www.imperium.ahlfeldt.se/places/') > -1) {
    return 'dare:' + uri.substr(39);
  } else if (uri.indexOf('http://gazetteer.dainst.org/place/') > -1) {
	return 'dai:' + uri.substr(34);
  } else if (uri.indexOf('http://sws.geonames.org/') > -1) {
	return 'geonames:' + uri.substr(24);
  } else if (uri.indexOf('http://vici.org/vici') > -1) {
	return 'vici:' + uri.substr(21); 
  } else {
    return uri;
  }
}

