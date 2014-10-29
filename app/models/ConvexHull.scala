package models

import play.api.db.slick.Config.driver.simple._
import com.vividsolutions.jts.geom.Geometry
import index.places.IndexedPlace

case class ConvexHull(geometry: Geometry) 

object ConvexHull {
  
  /** DB mapper function **/
  implicit val convexHullMapper = MappedColumnType.base[ConvexHull, String](
    { hull => hull.toString },
    { hull => ConvexHull.fromWKT(hull) })
  
  def fromIndexedPlaces(places: Seq[IndexedPlace]): ConvexHull = {
    null
  }
  
  def fromWKT(wkt: String): ConvexHull = {
    null
  }
  
}