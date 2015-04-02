package models.geo

import index.places.IndexedPlace
import com.vividsolutions.jts.geom.{ Envelope, Geometry }
import play.api.db.slick.Config.driver.simple._


case class BoundingBox(minLon: Double, maxLon: Double, minLat: Double, maxLat: Double) {
  
  override lazy val toString = Seq(minLon, maxLon, minLat, maxLat).mkString(",")
  
}

object BoundingBox {
  
  /** DB mapper function **/
  implicit val statusMapper = MappedColumnType.base[BoundingBox, String](
    { bbox => bbox.toString },
    { bbox => BoundingBox.fromString(bbox).get })
        
  /** Computes a bounding box from a list of geometries **/
  def compute(geometries: Seq[Geometry]): Option[BoundingBox] = {
    if (geometries.size > 0) {
      val envelope = new Envelope()
      geometries.foreach(geom => envelope.expandToInclude(geom.getEnvelopeInternal))
      Some(BoundingBox(envelope.getMinX, envelope.getMaxX, envelope.getMinY, envelope.getMaxY))
    } else {
      None
    }
  }
  
  /** Helper function to get the bounds of a list of places **/
  def fromPlaces(places: Seq[IndexedPlace]): Option[BoundingBox] =
    compute(places.flatMap(_.geometry))
    
  /** Helper function to parse a comma-separated string representation **/
  def fromString(s: String): Option[BoundingBox] = {
    val coords = s.split(",").map(_.trim)
    if (coords.size == 4) {
      try {
        Some(BoundingBox(coords(0).toDouble, coords(1).toDouble, coords(2).toDouble, coords(3).toDouble))
      } catch {
        case _:Throwable => None
      }
    } else {
      None
    }
  }
  
}
