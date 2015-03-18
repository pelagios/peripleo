define(function() {

  jQuery(document).ready(function() {
    var div = $('#graph'),
	      width = div.width(),
		    height = div.height(),
        
        relatedLinks = jQuery.grep($('link'), function(element) { return $(element).attr('rel') == 'related'; }),
        dataURL = $(relatedLinks[0]).attr('href'),

        force = d3.layout.force()
          .charge(-100)
          .linkDistance(80)
          .size([width, height]),
          
        zoom = d3.behavior.zoom()
          .scaleExtent([1, 10])
          .on('zoom',  function() {
            svg.attr('transform', 'translate(' + d3.event.translate + ') scale(' + d3.event.scale + ')'); }),
        
        svg = d3.select('#graph').append('svg')
		      .attr('width', width)
		      .attr('height', height)
          .call(zoom)
          .append('g'),
          
        renderGraph = function(nodes, edges) {
          var edge, node;
          
          force
            .nodes(nodes)
            .links(edges)
            .start();
          
          edge = svg.selectAll('.link')
		        .data(edges)
		        .enter().append('line')
            .attr('class', 'link')
            .style('stroke-width', function(d) { return 1.5 * Math.sqrt(d.weight); });              
            
          node = svg.selectAll('.node')
		        .data(nodes)
		        .enter().append('g')
		        .attr('class', 'node')

		        .call(force.drag);
            
          node.append('circle')
            .attr('r', function(d) {
              return Math.max(6, 3 * Math.sqrt(d.weight)); 
            });
            
          node.append('title').text(function(d) { return d.title; });
          node.append('text')
            .attr('x', 12)
            .attr('dy', '.35em')
            .text(function(d) { return d.title });
    
          force.on('tick', function() {
            edge
		          .attr('x1', function(d) { return d.source.x; })
		          .attr('y1', function(d) { return d.source.y; })
		          .attr('x2', function(d) { return d.target.x; })
		          .attr('y2', function(d) { return d.target.y; });
              
            node.attr('transform', function(d) { return 'translate(' + d.x + ',' + d.y + ')' });
		      }); 
          
        };
          
        loadGraphData = function(url, callback) {
          jQuery.getJSON(url, function(data) {
            callback(data.nodes, data.links);
          });
        };
        
    loadGraphData(dataURL, renderGraph);
  });        
    
    



                /*
		  svg.append('defs').selectAll('marker')
		    .data(['end'])
		    .enter().append('marker')
		      .attr('id', String)
		      .attr('viewBox', '0 -5 10 10')
		      .attr('refX', 16)
		      .attr('refY', 0)
		      .attr('markerWidth', 7)
		      .attr('markerHeight', 9)
		      .attr('orient', 'auto')
		      .append('path')
		        .attr('d', 'M0,-5L10,0L0,5');
		      

		      
		  node.append('circle')
		    .attr('r', 6)
		    .attr('class', function(d) { 
		        if (d.source_gazetteer) 
		          return d.source_gazetteer.toLowerCase(); 
		        else
		          return 'virtual';
		      });
		            
		  node.append('title')
		    .text(function(d) { return (d.title) ? d.title : d.uri; });
		      
		  node.append('text')
		    .attr('x', 12)
		    .attr('dy', '.35em')
		    .text(function(d) { return util.formatGazetteerURI(d.uri); });
		    
		  force.start();
      */

});
