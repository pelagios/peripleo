/** One 'facet dimension chart' block **/
define(function() {
  
  var FacetChart = function(parent, title, cssClass) {
    var header = jQuery(
          '<div class="facet-header">' +
          '  <h3>' + title + '</h3>' +
          '  <a class="filter" href="#"><span class="icon">&#xf0b0;</span><span class="label">Set Filter</span></a>' +
          '</div>'),
          
        list = jQuery(
          '<ul class="chart ' + cssClass + '"></ul>'),
          
        facetValTemplate = 
          '<li>' +
          '  <div class="meter"><div class="bar"></div><div class="label"></div></div>' +
          '</li>',
          
        /** Shorthand function for sorting facet values by count **/
        sortFacetValues = function(a,b) { return b.count - a.count },
        
        /** Shorthand function for formatting numbers **/
        formatNumber = function(number) { return numeral(number).format('0,0'); },
        
        formatFacetLabel = function(label) {
          // TODO optimize by handing the approprate formatting function on initialization
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
          
        update = function(facets) {
          var maxCount = (facets.length > 0) ? facets.slice().sort(sortFacetValues)[0].count : 0;
              
          list.empty();
          jQuery.each(facets, function(idx, val) {
            var row = jQuery(facetValTemplate),
                bar = row.find('.bar'),
                percentage = (100 * val.count / maxCount) + '%',
                label = formatFacetLabel(val.label);
              
            
            bar.css('width', percentage);
            bar.attr('title', formatNumber(val.count) + ' Results');
            row.find('.label').html(label);
            list.append(row);
          });
        };
    
    parent.append(header);
    parent.append(list);
    
    this.update = update;
  };
  
  return FacetChart;
  
});
