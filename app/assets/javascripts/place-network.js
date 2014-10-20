window.PlaceNetwork = function(divId, nodes, edges) {
  var div = $('#' + divId),
      width = div.width(),
      height = div.height(),
      innerNodes = $.grep(nodes, function(n) {
        return n.is_inner_node;
      }),
      innerEdges = $.grep(edges, function(e) {
        return e.is_inner_edge;
      });

  var force = d3.layout.force()
    .charge(-300)
    .linkDistance(60)
    .size([width, height])
    .nodes(nodes)
    .links(edges)
    .on('tick', function() {
      link
        .attr('x1', function(d) { return d.source.x; })
        .attr('y1', function(d) { return d.source.y; })
        .attr('x2', function(d) { return d.target.x; })
        .attr('y2', function(d) { return d.target.y; });
      
      node.attr('transform', function(d) { return 'translate(' + d.x + ',' + d.y + ')' });
    });
    
  var svg = d3.select('#' + divId).append('svg')
    .attr('width', width)
    .attr('height', height);
              
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
      
  var link = svg.selectAll('.link')
    .data(innerEdges)
    .enter().append('line')
    .attr('class', function(d) {
      var t = nodes[d.target];
      if (t.label)
        return 'link'
      else
        return 'link virtual';
    })
    .attr('marker-end', 'url(#end)')

  var node = svg.selectAll('.node')
    .data(innerNodes)
    .enter().append('g')
    .attr('class', 'node')
    .call(force.drag);
      
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
}
