package ingest

import global.Global
import java.util.UUID
import models.core.{ Annotation, AnnotatedThing, Dataset, Image }
import models.geo.Hull
import play.api.db.slick._
import scala.io.Source
import scala.xml.{ XML, Node, NodeSeq, Text }

object TEIImporter extends AbstractImporter {
  
  /** Helper to clean up text extracted from XML **/
  private def trim(str: String): String = {
    str.replaceAll("\\s\\s+", " ").trim
  }
  
  private def getTextBetween(fromNode: Option[Node], toNode: Option[Node], body: Node): Option[String] = {
    val children = body.descendant    
    
    val fromIdx = fromNode.map(n => {
      // Skip placeName node, plus all it's children
      val idx = children.indexOf(n)
      val numberOfInnerNodes = n.descendant.size
      (idx + numberOfInnerNodes + 1)
    }).getOrElse(0)
    
    val toIdx = toNode.map(n => children.indexOf(n)).getOrElse(children.size)
     
    val text = children.slice(fromIdx, toIdx).filter(_.isInstanceOf[Text]).map(_.text).mkString(" ").trim
    if (text.size > 0)
      Some(trim(text))
    else
      None
  }
  
  private def buildAnnotations(body: Node, thingId: String, dataset: Dataset): Seq[(Annotation, Option[String], Option[String])] = {
    val placeNameNodes = (body \\ "placeName")
    
    placeNameNodes.zipWithIndex.flatMap { case (node, idx) => {
      // TODO could be improved by caching results
      val gazetteerURI = node.attribute("ref").map(_.head.text)
      gazetteerURI.map(uri => {
        val annotation = Annotation(UUID.randomUUID, dataset.id, thingId, uri, Some(node.text), None)
        
        val prevNode: Option[Node] =
          if (idx > 0)
            Some(placeNameNodes(idx - 1))
          else
            None
        val prefix = getTextBetween(prevNode, Some(node), body)
          
        val nextNode: Option[Node] =
          if (idx < placeNameNodes.size - 1)
            Some(placeNameNodes(idx + 1))
          else
            None
        val suffix = getTextBetween(Some(node), nextNode, body)
        
        (annotation, prefix, suffix)
      })
    }}
  }
  
  def importTEI(source: Source, dataset: Dataset)(implicit s: Session) = {
    val teiXml = XML.loadString(source.getLines().mkString("\n"))
    val body = (teiXml \\ "body")(0)
    
    // TEI header metadata
    val title = (teiXml \\ "teiHeader" \\ "title").text
    val homepage = (teiXml \\ "teiHeader" \\ "sourceDesc" \\ "link").head.attribute("target").map(_.text)
    val thingId = sha256(dataset.id + " " + title + " " + homepage)
    
    // Places & geographic hull
    val places = 
      (teiXml \\ "placeName")
        .flatMap(_.attribute("ref").map(_.head.text))
        .groupBy(uri => uri).mapValues(_.size).toSeq
        .flatMap { case (uri, count) => 
          Global.index.findPlaceByAnyURI(uri).map(place => (place, uri, count)) }   

    val hull = Hull.fromPlaces(places.map(_._1))
    
    // Wrapped annotated thing
    val thing = AnnotatedThing(
        thingId, 
        dataset.id,
        title, 
        None, // Description
        None, // isPartOf
        homepage, 
        None, // TODO temporal bounds start
        None, // TODO temporal bounds end
        hull)
    
    // Fulltext
    val fulltext = trim(body.text)
    
    // Annotations, with context (i.e. fulltext before and after annotation)
    val annotationsWithText = buildAnnotations(body, thingId, dataset)
    
    // The wrapped ingest record (for TEI, we only have a single one)
    val ingestRecord = IngestRecord(thing, annotationsWithText, places, Some(fulltext), Seq.empty[Image])
    
    ingest(Seq(ingestRecord), dataset)
  }
  
}