package index.places

import com.vividsolutions.jts.io.WKTWriter
import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import index.IndexFields
import org.geotools.geojson.geom.GeometryJSON
import org.pelagios.api.PlainLiteral
import org.pelagios.api.gazetteer.PlaceCategory
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.pelagios.api.gazetteer.Place
import index.Index

class IndexedPlace(json: String) {
  
  override val toString = json
  
  lazy val asJson = Json.parse(json)
    
  lazy val uri: String = (asJson \ "uri").as[String] 
  
  lazy val sourceGazetteer: String = (asJson \ "source_gazetteer").as[String] 
  
  lazy val label: String = (asJson \ "label").as[String]
  
  lazy val description: Option[String] = (asJson \ "description").asOpt[String] 
  
  lazy val category: Option[PlaceCategory.Category] = ((asJson \ "category").asOpt[String]).map(PlaceCategory.withName(_))
  
  lazy val names: List[PlainLiteral] = (asJson \ "names").as[List[JsObject]]
    .map(literal => {
      val chars = (literal \ "chars").as[String]
      val lang = (literal \ "lang").asOpt[String]
      PlainLiteral(chars, lang)
    })
  
  lazy val geometryJson: Option[JsValue] = (asJson \ "geometry").asOpt[JsValue]
  
  lazy val geometry: Option[Geometry] = geometryJson
    .map(geoJson => new GeometryJSON().read(Json.stringify(geoJson).trim))
    
  lazy val geometryWKT: Option[String] =  geometry.map(geom => new WKTWriter().write(geom))
  
  lazy val centroid: Option[Coordinate] = geometry.map(_.getCentroid.getCoordinate)
  
  lazy val closeMatches: List[String] = (asJson \ "close_matches").as[List[String]]
  
  lazy val exactMatches: List[String] = (asJson \ "exact_matches").as[List[String]]
  
  lazy val matches: List[String] = closeMatches ++ exactMatches
  
}

object IndexedPlace { 
   
  /** JSON Writes **/
  
  private implicit val plainLiteralWrites: Writes[PlainLiteral] = (
    (JsPath \ "chars").write[String] ~
    (JsPath \ "lang").writeNullable[String]
  )(l => (l.chars, l.lang))
  
  private implicit val placeWrites: Writes[Place] = (
    (JsPath \ "uri").write[String] ~
    (JsPath \ "label").write[String] ~
    (JsPath \ "description").writeNullable[String] ~
    (JsPath \ "category").writeNullable[String] ~
    (JsPath \ "names").write[Seq[PlainLiteral]] ~
    (JsPath \ "geometry").writeNullable[JsValue] ~
    (JsPath \ "close_matches").write[Seq[String]] ~
    (JsPath \ "exact_matches").write[Seq[String]]
  )(p  => (
      Index.normalizeURI(p.uri),
      p.label,
      p.descriptions.headOption.map(_.chars),
      p.category.map(_.toString),
      p.names,
      p.locations.headOption.map(location => Json.parse(location.geoJSON)),
      p.closeMatches.map(Index.normalizeURI(_)),
      p.exactMatches.map(Index.normalizeURI(_))))
  
  private implicit val placeFromGazetteerWrites: Writes[(Place, String)] = (
    (JsPath).write[Place] ~
    (JsPath \ "source_gazetteer").write[String]
  )(t => (t._1, t._2.toLowerCase))
  
  def toIndexedPlace(place: Place, sourceGazetteer: String): IndexedPlace = {
    val json = Json.toJson((place, sourceGazetteer))
    new IndexedPlace(Json.stringify(json))
  }
  
}