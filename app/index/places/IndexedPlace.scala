package index.places

import com.vividsolutions.jts.geom.Coordinate
import index.IndexFields
import org.geotools.geojson.geom.GeometryJSON
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.pelagios.api.PlainLiteral
import org.pelagios.api.gazetteer.PlaceCategory

case class IndexedPlace(
    
  uri: String, 
  
  sourceGazetteer: String, 
  
  title: String, 
  
  description: Option[String], 
  
  category: Option[PlaceCategory.Category],
  
  names: Seq[PlainLiteral],
  
  geometry: Option[GeoJSON],
  
  closeMatches: Seq[String])

object IndexedPlace { 
  
  /** JSON Reads **/
  
  implicit val placeCategoryReads: Reads[PlaceCategory.Category] = 
    (JsPath).read[String].map(PlaceCategory.withName(_))
    
  implicit val geojsonReads: Reads[GeoJSON] =
    (JsPath).read[JsValue].map(json => GeoJSON(Json.stringify(json)))
  
  implicit val plainLiteralReads: Reads[PlainLiteral] = (
    (JsPath \ "chars").read[String] ~
    (JsPath \ "lang").readNullable[String]
  )(PlainLiteral.apply _)
  
  implicit val placeReads: Reads[IndexedPlace] = (
    (JsPath \ "uri").read[String] ~
    (JsPath \ "source_gazetteer").read[String] ~
    (JsPath \ "title").read[String] ~
    (JsPath \ "description").readNullable[String] ~
    (JsPath \ "category").readNullable[PlaceCategory.Category] ~
    (JsPath \ "names").read[Seq[PlainLiteral]] ~
    (JsPath \ "geometry").readNullable[GeoJSON] ~
    (JsPath \ "close_matches").read[Seq[String]]
  )(IndexedPlace.apply _)
  
  /** JSON Writes **/
  
  implicit val plainLiteralWrites: Writes[PlainLiteral] = (
    (JsPath \ "chars").write[String] ~
    (JsPath \ "lang").writeNullable[String]
  )(l => (l.chars, l.lang))
  
  implicit val placeWrites: Writes[IndexedPlace] = (
    (JsPath \ "uri").write[String] ~
    (JsPath \ "source_gazetteer").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "description").writeNullable[String] ~
    (JsPath \ "category").writeNullable[String] ~
    (JsPath \ "names").write[Seq[PlainLiteral]] ~
    (JsPath \ "geometry").writeNullable[JsValue] ~
    (JsPath \ "close_matches").write[Seq[String]]
  )(p => (
      p.uri,
      p.sourceGazetteer,
      p.title,
      p.description,
      p.category.map(_.toString),
      p.names,
      p.geometry.map(_.asJSON),
      p.closeMatches))
  
}

case class GeoJSON(private val json: String) {
  
  override val toString = json
  
  val asJSON = Json.parse(json)
  
  lazy val geom = new GeometryJSON().read(json.trim)
    
  lazy val centroid: Coordinate = geom.getCentroid.getCoordinate
  
}