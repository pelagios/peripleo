require([], function() {
  
  jQuery(document).ready(function() {
    var resultStats = jQuery('#result-stats'), 
    
        awmcLayer = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Data &copy; <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
          '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC</a>'}),    
          
        facetValueTemplate = 
          '<tr>' +
          '  <td class="label"></td>' +
          '  <td class="count-bar"><div class="meter"><div class="bar"></div></div></td>' +
          '  <td class="count-number"></td>' +
          '</tr>',

        typeChartTable = jQuery('#type-chart'),
        
        sourceChartTable = jQuery('#source-chart'),
        
        /** Shorthands **/
        formatNumber = function(number) { return numeral(number).format('0,0'); },
        sortFacetValues = function(a,b) { return b.count - a.count },
        
        /** Helper to parse the source facet label **/
        parseSourceFacetLabel = function(labelAndId) { 
          var separatorIdx = labelAndId.indexOf('#'),
              label = labelAndId.substring(0, separatorIdx),
              id = labelAndId.substring(separatorIdx);
              
          return { label: label, id: id };
        },
        
        map = new L.Map('map', {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 3,
          layers: [ awmcLayer],
          zoomControl:false
        }),
        
        pendingRequest = false,

        updateFacets = function(facets) {
          // UI currently hard-wired to show 'type' and 'source dataset' facets only
          var typeDim = jQuery.grep(facets, function(facet) { return facet.dimension === 'type'; }),
              typeFacets = (typeDim.length > 0) ? typeDim[0].top_children : false,
              typeMaxCount = (typeFacets) ? typeFacets.sort(sortFacetValues)[0].count : 0,
              
              sourceDim = jQuery.grep(facets, function(facet) { return facet.dimension === 'dataset'; }),
              sourceFacets = (sourceDim.length > 0) ? sourceDim[0].top_children : false;     
              sourceMaxCount = (sourceFacets) ? sourceFacets.sort(sortFacetValues)[0].count : 0,
              
          typeChartTable.empty();
          if (typeFacets) {
            jQuery.each(typeFacets, function(idx, val) {
              var row = jQuery(facetValueTemplate),
                  percentage = (100 * val.count / typeMaxCount) + '%';
              
              row.find('.label').html(val.label);
              row.find('.bar').css('width', percentage);
              row.find('.count-number').html(formatNumber(val.count));
              typeChartTable.append(row);
            });
          }
          
          sourceChartTable.empty();
          if (sourceFacets) {
            jQuery.each(sourceFacets, function(idx, val) {
              var row = jQuery(facetValueTemplate),
                  labelAndId = parseSourceFacetLabel(val.label),
                  percentage = (100 * val.count / sourceMaxCount) + '%';
              
              row.find('.label').html(labelAndId.label);
              row.find('.bar').css('width', percentage);
              row.find('.count-number').html(formatNumber(val.count));
              sourceChartTable.append(row);              
            });
          }
        },
        
        updateTimeHistogram = function(values) {
          
        },
        
        updateCount = function(e) {
          var b = map.getBounds(),
              bboxParam = b.getWest() + ',' + b.getEast() + ',' + b.getSouth() + ',' + b.getNorth();
          
          if (!pendingRequest) {    
            pendingRequest = true;
            jQuery.getJSON('/api-v3/search?facets=true&timehistogram=true&bbox=' + bboxParam, function(response) {
              resultStats.html(formatNumber(response.total) + ' Results');   
              updateFacets(response.facets);      
              updateTimeHistogram(response.time_histogram);
            })
            .always(function() {
              pendingRequest = false;
            });
          }
        };

        
    map.on('move', updateCount);
    updateCount();
        
  });
  
});
