package index.places

import com.vividsolutions.jts.geom.Coordinate
import index._
import org.apache.lucene.index.IndexableField
import org.apache.lucene.document.{ Document, Field, StringField, TextField }
import org.geotools.geojson.geom.GeometryJSON
import org.pelagios.api.PlainLiteral
import org.pelagios.api.gazetteer.{ Place, PlaceCategory }
import play.api.libs.json.Json
import scala.collection.JavaConversions._

case class IndexedPlace(private[places] val doc: Document) {
  
  val uri: String = doc.get(IndexFields.PLACE_URI)
  
  val title: String = doc.get(IndexFields.TITLE)
    
  val description: Option[String] = Option(doc.get(IndexFields.DESCRIPTION))
    
  val names: Seq[PlainLiteral] = 
    doc.getFields().filter(_.name.startsWith(IndexFields.PLACE_NAME)).map(field => IndexedPlace.toPlainLiteral(field))
  
  val category: Option[PlaceCategory.Category] = Option(doc.get(IndexFields.PLACE_CATEGORY)).map(PlaceCategory.withName(_))

  val geometry: Option[GeoJSON] = Option(doc.get(IndexFields.PLACE_GEOMETRY)).map(GeoJSON(_))
  
  val seedURI: String = doc.get(IndexFields.PLACE_SEED_URI)
  
}

object IndexedPlace {
  
  def toDoc(place: Place, seedURI: Option[String]): Document = {
    val doc = new Document()
    doc.add(new StringField(IndexFields.PLACE_URI, Index.normalizeURI(place.uri), Field.Store.YES))
    doc.add(new TextField(IndexFields.TITLE, place.title, Field.Store.YES))
    place.descriptions.foreach(description => doc.add(new TextField(IndexFields.DESCRIPTION, description.chars, Field.Store.YES)))
    place.names.foreach(name => {
      val fieldName = name.lang.map(IndexFields.PLACE_NAME + "@" + _).getOrElse(IndexFields.PLACE_NAME)
      doc.add(new TextField(fieldName, name.chars, Field.Store.YES))
    })
    place.locations.foreach(location => doc.add(new StringField(IndexFields.PLACE_GEOMETRY, location.geoJSON, Field.Store.YES)))
    if (place.category.isDefined)
      doc.add(new StringField(IndexFields.PLACE_CATEGORY, place.category.get.toString, Field.Store.YES))
    place.closeMatches.foreach(closeMatch => doc.add(new StringField(IndexFields.PLACE_CLOSE_MATCH, Index.normalizeURI(closeMatch), Field.Store.YES)))
    doc.add(new StringField(IndexFields.PLACE_SEED_URI, seedURI.getOrElse(place.uri), Field.Store.YES))
    doc    
  }
  
  def toPlainLiteral(field: IndexableField): PlainLiteral = {
    val language = if (field.name.indexOf('@') > -1) Some(field.name.substring(field.name.indexOf('@') + 1)) else None
    PlainLiteral(field.stringValue(), language)    
  }
  
}

case class GeoJSON(private val json: String) {
  
  override val toString = json
  
  val asJSON = Json.parse(json)
  
  lazy val geom = new GeometryJSON().read(json.trim)
    
  lazy val centroid: Coordinate = geom.getCentroid.getCoordinate
  
}