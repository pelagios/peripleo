package models

import org.geotools.geojson.geom.GeometryJSON
import play.api.libs.json.Json
import play.api.db.slick.Config.driver.simple._
import index.places.GeoJSON

/** GazetteerReference model class.
  * 
  * Note: a gazetteer reference caches some information that normally resides in the 
  * gazetteer index. This way, we don't always have to introduce an extra index resolution
  * step when retrieving place URIs from the database.
  */
case class GazetteerReference(uri: String, title: String, geometry: Option[GeoJSON])

trait HasGeoJSONColumn {
  
  implicit val geoJsonMapper = MappedColumnType.base[GeoJSON, String](
    { geojson => geojson.toString },
    { geojson => GeoJSON(geojson) })

  
}