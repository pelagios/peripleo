/** One 'facet dimension chart' block **/
define(function() {
  
  var FacetChart = function(parent, title, cssClass) {
    var header = jQuery(
          '<tbody class="thead">' +
          '  <tr><td colspan="2"><h3 class="facet-dimension">' + title + '</h3></td></tr>' +
          '</tbody>'),
          
        body = jQuery(
          '<tbody class="chart ' + cssClass + '"></tbody>'),
          
        facetValTemplate = 
          '<tr>' +
          '  <td class="label"></td>' +
          '  <td class="count-bar">' +
          '    <div class="meter"><div class="bar"></div><span class="count-number"></span></div>' +
          '  </td>' +
          '</tr>',
          
        /** Shorthand function for sorting facet values by count **/
        sortFacetValues = function(a,b) { return b.count - a.count },
        
        /** Shorthand function for formatting numbers **/
        formatNumber = function(number) { return numeral(number).format('0,0'); },
          
        update = function(facets) {
          var maxCount = (facets.length > 0) ? facets.sort(sortFacetValues)[0].count : 0;
              
          body.empty();
          jQuery.each(facets, function(idx, val) {
            var row = jQuery(facetValTemplate),
                percentage = (100 * val.count / maxCount) + '%';
              
            row.find('.label').html(val.label);
            row.find('.bar').css('width', percentage);
            row.find('.count-number').html(formatNumber(val.count));
            body.append(row);
          });
        };
    
    parent.append(header);
    parent.append(body);
    
    this.update = update;
  };
  
  return FacetChart;
  
});
