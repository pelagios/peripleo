package models

import play.api.db.slick.Config.driver.simple._
import com.vividsolutions.jts.geom.{ Envelope, Geometry }

case class BoundingBox(minLon: Double, maxLon: Double, minLat: Double, maxLat: Double) {
  
  override lazy val toString = Seq(minLon, maxLon, minLat, maxLat).mkString(",")
  
}

object BoundingBox {
  
  implicit val statusMapper = MappedColumnType.base[BoundingBox, String](
    { bbox => bbox.toString },
    { bbox => BoundingBox.parseString(bbox).get })
    
  def compute(geometries: Seq[Geometry]): Option[BoundingBox] = {
    if (geometries.size > 0) {
      val envelope = new Envelope()
      geometries.foreach(geom => envelope.expandToInclude(geom.getEnvelopeInternal))
      Some(BoundingBox(envelope.getMinX, envelope.getMaxX, envelope.getMinY, envelope.getMaxY))
    } else {
      None
    }
  }
    
  def parseString(s: String): Option[BoundingBox] = {
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
