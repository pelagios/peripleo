package ingest

import models.core.Dataset
import play.api.db.slick._
import scala.io.Source
import scala.xml.XML
object TEImporter extends AbstractImporter {
  
  def importTEI(source: Source, dataset: Dataset)(implicit s: Session) = {
    val teiXml = XML.loadString(source.getLines().mkString("\n"))
    
    // TEI header metadata
    val title = (teiXml \\ "teiHeader" \\ "title").text
    val homepage = (teiXml \\ "teiHeader" \\ "sourceDesc" \\ "link").head.attribute("target")
    val id = sha256(dataset.id + " " + title + " " + homepage)
    
    val placeNames = (teiXml \\ "placeName").flatMap(node =>
          node.attribute("ref").map(uri => (uri.head.text, node.text)))
        .filter(!_._1.endsWith("None"))
    
    println(placeNames)
    
    // TODO compute hull (after we have all the locations
    
    /*
    id: String, 
    dataset: String, 
    title: String, 
    description: Option[String],
    isPartOf: Option[String],
    homepage: Option[String],
    temporalBoundsStart: Option[Int],
    temporalBoundsEnd: Option[Int],  
    hull: Option[Hull])
    */
  }
  
}