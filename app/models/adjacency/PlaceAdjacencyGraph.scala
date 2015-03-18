package models.adjacency

import models.geo.GazetteerReference
import play.api.Logger

class PlaceAdjacencyGraph(adjacencies: Seq[PlaceAdjacency]) {

  case class Edge(from: Int, to: Int, weight: Int) 
  
  val nodes: Seq[GazetteerReference] = adjacencies.flatMap(a => Seq(a.place, a.nextPlace)).distinct
  
  val edges: Seq[Edge] = adjacencies.map(pair =>
      Edge(nodes.indexOf(pair.place), nodes.indexOf(pair.nextPlace), pair.weight))
     
  Logger.info(nodes.toString)
      
}