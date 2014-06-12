window.PlaceNetwork = function(divId, network) {
  var div = $('#' + divId),
      width = div.width(),
      height = div.height();

  var transform = function(d) {
    return 'translate(' + d.x + ',' + d.y + ')';
  };
  
  var force = d3.layout.force()
    .charge(-300)
    .linkDistance(120)
    .size([width, height])
    .nodes(network.nodes)
    .links(network.edges)
    .start();

  var svg = d3.select('#' + divId).append('svg')
    .attr('width', width)
    .attr('height', height);
              
  svg.append('defs').selectAll('marker')
    .data(['end'])      // Different link/path types can be defined here
    .enter().append('marker')    // This section adds in the arrows
      .attr('id', String)
      .attr('viewBox', '0 -5 10 10')
      .attr('refX', 15)
      .attr('refY', -1.5)
      .attr('markerWidth', 8)
      .attr('markerHeight', 8)
      .attr('orient', 'auto')
      .append('path')
        .attr('d', 'M0,-5L10,0L0,5');
      
  var link = svg.selectAll('.link')
    .data(network.edges)
    .enter().append('line')
    .attr('class', 'link')
    .attr('marker-end', 'url(#end)')
    .style('stroke', '#ccc')
    .style('stroke-width', function(d) { return 1; })

  var node = svg.selectAll('.node')
    .data(network.nodes)
    .enter().append('g')
    .attr('class', 'node')
    .call(force.drag);
      
  node.append('circle')
    .attr('r', 5)
    .style('fill', function(d) { return '#fff000'; });
      
  node.append('title')
    .text(function(d) { return (d.title) ? d.title : d.uri; });
      
  node.append('text')
    .attr('x', 12)
    .attr('dy', '.35em')
    .text(function(d) { return util.formatGazetteerURI(d.uri); });
     
  force.on('tick', function() {
    link
      .attr('x1', function(d) { return d.source.x; })
      .attr('y1', function(d) { return d.source.y; })
      .attr('x2', function(d) { return d.target.x; })
      .attr('y2', function(d) { return d.target.y; });
      
    node.attr('transform', transform);
  });
}
