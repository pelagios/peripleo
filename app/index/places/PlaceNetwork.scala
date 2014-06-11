package index.places

case class PlaceNetwork(private val places: Seq[IndexedPlace], private val links: Seq[(String, String)]) {
  
  private val _seedURIs = places.groupBy(_.seedURI).keys
  
  if (_seedURIs.size != 1)
    throw new IllegalArgumentException("Network contains places with different seed URIs: " + _seedURIs.mkString(", "))
  
  val seedURI = _seedURIs.head
  
  val nodes = (links.map(_._1) ++ links.map(_._2)).distinct.map(uri => NetworkNode(uri, places.find(_.uri == uri)))
  
  val edges = links.map { case (source, target) => 
    NetworkEdge(nodes.indexWhere(_.uri == source), nodes.indexWhere(_.uri == target)) }
  
  /** Networks are equal if their seed URI is equal **/
  override def equals(o: Any) = o match{
    case other: PlaceNetwork => other.seedURI.equals(seedURI)
    case _ => false
  }
  
  override def hashCode = seedURI.hashCode()

}

case class NetworkNode(uri: String, place: Option[IndexedPlace])

case class NetworkEdge(source: Int, target: Int)
