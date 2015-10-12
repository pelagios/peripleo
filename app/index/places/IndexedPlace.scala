package index.places

import com.vividsolutions.jts.io.WKTWriter
import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import index.IndexFields
import java.util.{ Calendar, Date }
import org.geotools.geojson.geom.GeometryJSON
import org.pelagios.api.PlainLiteral
import org.pelagios.api.gazetteer.PlaceCategory
import org.pelagios.api.gazetteer.patch.{ PlacePatch, PatchConfig, PatchStrategy }
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.pelagios.api.gazetteer.Place
import index.Index

class IndexedPlace(json: String) {
  
  override val toString = json
  
  lazy val asJson = Json.parse(json).as[JsObject]
    
  lazy val uri: String = (asJson \ "uri").as[String] 
  
  lazy val sourceGazetteer: String = (asJson \ "source_gazetteer").as[String] 
  
  lazy val label: String = (asJson \ "label").as[String]
  
  lazy val description: Option[String] = (asJson \ "description").asOpt[String] 
  
  lazy val category: Option[PlaceCategory.Category] = ((asJson \ "category").asOpt[String]).map(PlaceCategory.withName(_))
  
  lazy val names: Seq[PlainLiteral] = (asJson \ "names").as[List[JsObject]]
    .map(literal => {
      val chars = (literal \ "chars").as[String]
      val lang = (literal \ "lang").asOpt[String]
      PlainLiteral(chars, lang)
    })
    
  lazy val depictions: Seq[String] = (asJson \ "depictions").asOpt[List[String]].getOrElse(List.empty[String])
    
  lazy val temporalBoundsStart: Option[Int] = (asJson \ "temporal" \ "from").asOpt[Int]
  
  lazy val temporalBoundsEnd: Option[Int] = (asJson \ "temporal" \ "to").asOpt[Int]
  
  lazy val geometryJson: Option[JsValue] = (asJson \ "geometry").asOpt[JsValue]
  
  lazy val geometry: Option[Geometry] = geometryJson
    .map(geoJson => new GeometryJSON().read(Json.stringify(geoJson).trim))
    
  lazy val geometryWKT: Option[String] =  geometry.map(geom => new WKTWriter().write(geom))
  
  lazy val centroid: Option[Coordinate] = geometry.map(_.getCentroid.getCoordinate)
  
  lazy val closeMatches: Seq[String] = (asJson \ "close_matches").as[List[String]]
  
  lazy val exactMatches: Seq[String] = (asJson \ "exact_matches").as[List[String]]
  
  lazy val matches: Seq[String] = closeMatches ++ exactMatches
  
  def patch(patch: PlacePatch, config: PatchConfig): IndexedPlace = 
    patchGeometry(patch, config.geometryStrategy).patchNames(patch, config.namesStrategy)
    
  private def patchGeometry(patch: PlacePatch, strategy: PatchStrategy.Value): IndexedPlace = {
    val geometry = patch.location.map(location => Json.parse(location.asGeoJSON))
    if (geometry.isDefined)
      strategy match {
        case PatchStrategy.REPLACE => {
          val updatedJson = (asJson - "geometry") + ("geometry" -> geometry.get) 
          new IndexedPlace(Json.stringify(updatedJson))
        }
        case PatchStrategy.APPEND => throw new UnsupportedOperationException // We don't support append at this time
      }
    else
      this
  }
  
  private def patchNames(patch: PlacePatch, strategy: PatchStrategy.Value): IndexedPlace = {    
    if (patch.names.size > 0) {
      import IndexedPlace.plainLiteralWrites
      val names = Json.toJson(patch.names).as[JsArray] 
      
      strategy match {
        case PatchStrategy.REPLACE => {
          val updatedJson = (asJson - "names") + ("names" -> names)
          new IndexedPlace(Json.stringify(updatedJson))  
        }
        
        case PatchStrategy.APPEND => {
          val updatedNames = (asJson \ "names").as[JsArray] ++ names
          val updatedJson = (asJson - "names") + ("names" -> updatedNames)
          new IndexedPlace(Json.stringify(updatedJson))            
        }
      }
    } else {
      this
    }
  }
  
}

object IndexedPlace { 
   
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
    (JsPath \ "depictions").writeNullable[Seq[String]] ~
    (JsPath \ "temporal").writeNullable[JsValue] ~
    (JsPath \ "geometry").writeNullable[JsValue] ~
    (JsPath \ "close_matches").write[Seq[String]] ~
    (JsPath \ "exact_matches").write[Seq[String]]
  )(p  => (
      Index.normalizeURI(p.uri),
      p.label,
      p.descriptions.headOption.map(_.chars),
      p.category.map(_.toString),
      p.names,
      { if (p.depictions.size == 0) None else Some(p.depictions.map(_.uri)) },
      p.temporalCoverage.map(period => {
        val startYear = period.startYear
        val endYear = period.endYear.getOrElse(startYear)
        Json.obj("from" -> startYear, "to" -> endYear)
      }),
      p.location.map(location => Json.parse(location.asGeoJSON)),
      p.closeMatches.map(Index.normalizeURI(_)),
      p.exactMatches.map(Index.normalizeURI(_))))
  
  private implicit val placeFromGazetteerWrites: Writes[(Place, String)] = (
    (JsPath).write[Place] ~
    (JsPath \ "source_gazetteer").write[String]
  )(t => (t._1, t._2))
  
  def toIndexedPlace(place: Place, sourceGazetteer: String): IndexedPlace = {    
    val json = Json.toJson((place, sourceGazetteer))
    new IndexedPlace(Json.stringify(json))
  }
  
}