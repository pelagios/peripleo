window.PlaceNetwork = function(divId, network) {
  var div = $('#' + divId),
      width = div.width(),
      height = div.height();
  
  var force = d3.layout.force()
    .charge(-120)
    .linkDistance(80)
    .size([width, height]);

  var svg = d3.select('#' + divId).append('svg')
    .attr('width', width)
    .attr('height', height);
              
  force
    .nodes(network.nodes)
    .links(network.edges)
    .start();
      
  var link = svg.selectAll('.link')
    .data(network.edges)
    .enter().append('line')
    .attr('class', 'link')
    .style('stroke', '#ccc')
    .style('stroke-width', function(d) { return 1; });

  var node = svg.selectAll('.node')
    .data(network.nodes)
    .enter().append('g')
    .attr('class', 'node')
    .call(force.drag);
      
  node.append('circle')
    .attr('r', 5)
    .style('fill', function(d) { return '#fff000'; });
      
  node.append("title")
    .text(function(d) { return d.uri; });
      
  node.append('text')
    .attr('x', 12)
    .attr('dy', '.35em')
    .text(function(d) { return (d.title) ? d.title : d.uri; });
     
  force.on('tick', function() {
    link
      .attr('x1', function(d) { return d.source.x; })
      .attr('y1', function(d) { return d.source.y; })
      .attr('x2', function(d) { return d.target.x; })
      .attr('y2', function(d) { return d.target.y; });

    node.attr('transform', function(d) { return 'translate(' + d.x + ',' + d.y + ')'; });
  });
}
