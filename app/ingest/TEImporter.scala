package ingest

import java.util.UUID
import models.core.{ Annotation, AnnotatedThing, Dataset }
import play.api.db.slick._
import scala.io.Source
import scala.xml.{ XML, Node, NodeSeq, Text }
import global.Global

object TEImporter extends AbstractImporter {
  
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
      Some(text.replaceAll("\\s\\s+", " "))
    else
      None
  }
  
  private def buildAnnotations(body: Node, thingId: String, dataset: Dataset): Seq[(Option[String], Annotation, Option[String])] = {
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
        
        (prefix, annotation, suffix)
      })
    }}
  }
  
  def importTEI(source: Source, dataset: Dataset)(implicit s: Session) = {
    val teiXml = XML.loadString(source.getLines().mkString("\n"))
    
    // TEI header metadata
    val title = (teiXml \\ "teiHeader" \\ "title").text
    val homepage = (teiXml \\ "teiHeader" \\ "sourceDesc" \\ "link").head.attribute("target")
    val thingId = sha256(dataset.id + " " + title + " " + homepage)
    
    val placeNames = (teiXml \\ "placeName").flatMap(node =>
          node.attribute("ref").map(uri => (uri.head.text, node.text)))
        .filter(!_._1.endsWith("None"))
    
    val annotationsWithContext = buildAnnotations((teiXml \\ "body")(0), thingId, dataset)
    
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