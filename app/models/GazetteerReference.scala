package models

import org.geotools.geojson.geom.GeometryJSON
import play.api.libs.json.Json
import play.api.db.slick.Config.driver.simple._

/** GazetteerReference model class.
  * 
  * Note: a gazetteer reference caches some information that normally resides in the 
  * gazetteer index. This way, we don't always have to introduce an extra index resolution
  * step when retrieving place URIs from the database.
  */
case class GazetteerReference(uri: String, title: String, geometry: Option[GeoJSON])

/** GeoJSON model class.
  * 
  * Note: a GeoJSON geometry is really just a string, but we wrap it into a 
  * class, so we can later identify it properly (and in a type-safe manner). 
  */
case class GeoJSON(private val json: String) {
  
  override val toString = json
  
  val asJSON = Json.parse(json)
  
  lazy val geometry = new GeometryJSON().read(json.trim)
    
}

object GeoJSON {
    
  implicit val geoJsonMapper = MappedColumnType.base[GeoJSON, String](
    { geojson => geojson.toString },
    { geojson => GeoJSON(geojson) })
    
}