require(['common/autocomplete', 'common/densityGrid', 'common/timeHistogram'], function(AutoComplete, DensityGrid, TimeHistogram) {
  
  jQuery(document).ready(function() {
    var searchForm = jQuery('#text-query form'),
        searchInput = jQuery('#query'),
        
        resultStats = jQuery('#result-stats'), 

        typeChartTable = jQuery('#type-chart'),
        
        sourceChartTable = jQuery('#source-chart'),
            
        queryFilters = {
          
          query: false,
          
          objectType: false,
          
          dataset: false,
          
          timespan: false
          
        },
        
        timeHistogram = new TimeHistogram('time-histogram', function(interval) {
          queryFilters.timespan = interval;
          update();
          refreshHeatmap();
        }),
        
        normalizeBounds = function(b) {
          var w = (b.getWest() < -179) ? -180 : b.getWest() - 1,
              e = (b.getEast() > 179) ? 180 : b.getEast() + 1,
              s = (b.getSouth() < -89) ? -90 : b.getSouth() - 1,
              n = (b.getNorth() > 89) ? 90 : b.getNorth() + 1;
              
          return { north: n, east: e, south: s, west: w };
        },
        
        // TODO no need to rebuild for every request - cache
        buildQueryURL = function() {
          var url = '/api-v3/search?facets=true&heatmap=true';
          
          if (queryFilters.query)
            url += '&query=' + queryFilters.query;
            
          if (queryFilters.objectType)
            url += '&type=' + queryFilters.objectType;
            
          if (queryFilters.dataset)
            url += '&dataset=' + queryFilters.dataset;
            
          if (queryFilters.timespan)
            url += '&from=' + queryFilters.timespan.from + '&to=' + queryFilters.timespan.to;
          
          return url + '&bbox='
        }
        
        autoComplete = new AutoComplete(searchForm, searchInput);
    
        awmcLayer = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Data &copy; <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
          '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC</a>'}),  
          
        dareLayer = L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
          attribution: 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>',
          minZoom:3,
          maxZoom:11
        }), 
          
        facetValueTemplate = 
          '<tr>' +
          '  <td class="label"></td>' +
          '  <td class="count-bar">' +
          '    <div class="meter"><div class="bar"></div><span class="count-number"></span></div>' +
          '  </td>' +
          '</tr>',

        
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
          layers: [ awmcLayer ],
          zoomControl:false
        }),
        
        densityGrid = new DensityGrid().addTo(map);
        
        pendingRequest = false,
        
        search = function() {
          queryFilters.query = searchInput.val();
          searchInput.blur();
          update();
          refreshHeatmap();
          return false; // preventDefault + stopPropagation
        },

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
        
        update = function(e) {
          var b = map.getBounds(),
              bboxParam = b.getWest() + ',' + b.getEast() + ',' + b.getSouth() + ',' + b.getNorth(),
              from = (queryFilters.timespan) ? queryFilters.timespan.from : false,
              to = (queryFilters.timespan) ? queryFilters.timespan.to : false;
          
          if (!pendingRequest) {    
            pendingRequest = true;
            jQuery.getJSON(buildQueryURL() + bboxParam + '&timehistogram=true', function(response) {
              resultStats.html(formatNumber(response.total) + ' Results');   
              updateFacets(response.facets);     
              timeHistogram.update(response.time_histogram, from, to);
            })
            .always(function() {
              pendingRequest = false;
            });
          }
        },
        
        refreshHeatmap = function(e) {
          var b = normalizeBounds(map.getBounds()),
              bboxParam = b.west + ',' + b.east + ',' + b.south + ',' + b.north;
 
          jQuery.getJSON(buildQueryURL() + bboxParam, function(response) {
            densityGrid.update(response.heatmap); 
          });
        };

        
    map.on('move', update);
    map.on('moveend', refreshHeatmap);
    update();
    refreshHeatmap();
        
    searchForm.submit(search);
    searchForm.keypress(function (e) {
      if (e.which == 13)
        searchForm.submit();
    });
  });
  
});
