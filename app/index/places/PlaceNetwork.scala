package index.places

case class PlaceNetwork(places: Seq[IndexedPlace], links: Seq[(String, String)]) {
  
  private val _seedURIs = places.groupBy(_.seedURI).keys
  
  if (_seedURIs.size != 1)
    throw new IllegalArgumentException("Network contains places with different seed URIs: " + _seedURIs.mkString(", "))
  
  val seedURI = _seedURIs.head
  
  /** Networks are equal if their seed URI is equal **/
  override def equals(o: Any) = o match{
    case other: PlaceNetwork => other.seedURI.equals(seedURI)
    case _ => false
  }
  
  override def hashCode = seedURI.hashCode()

}