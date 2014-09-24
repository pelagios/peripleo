package controllers.common.io

import java.util.UUID
import java.util.regex.Pattern
import models._
import play.api.db.slick._
import scala.io.Source
import global.Global
import play.api.Logger

object CSVImporter extends AbstractImporter {
  
  private val SEPARATOR = ";"
    
  private val SPLIT_REGEX = "(?<!\\\\)" + Pattern.quote(SEPARATOR)

  def importRecogitoCSV(source: Source, dataset: Dataset)(implicit s: Session) = {
    val data = source.getLines.toSeq
    
    val meta = toMap(data.takeWhile(_.startsWith("#")))  
    val header = data.drop(meta.size).take(1).toSeq.head.split(SEPARATOR, -1).toSeq
    
    // Recogito CSVs represent exactly one top-level AnnotatedThing...
    val parentThingIdPlain = dataset.id + " " + meta.get("author").getOrElse("") + meta.get("title").get + " " + meta.get("language").getOrElse("")
    val parentThing = AnnotatedThing(sha256(parentThingIdPlain), dataset.id, meta.get("title").get, None, None, None, None)
                  
    val uuidIdx = header.indexOf("uuid")
    val annotationsByPart = data.drop(meta.size + 1).map(_.split(SPLIT_REGEX, -1)).map(fields => {
      val uuid = if (uuidIdx > -1) UUID.fromString(fields(uuidIdx)) else UUID.randomUUID 
      val gazetteerURI = fields(header.indexOf("gazetteer_uri"))
      val documentPart = fields(header.indexOf("document_part"))
      
      (uuid, documentPart, gazetteerURI)     
    }).groupBy(_._2)
    
    val parts = annotationsByPart.keys.map(title =>
      AnnotatedThing(sha256(parentThing.id + " " + title), dataset.id, title, Some(parentThing.id), None, None, None))
    
    // TODO make use of 'quote' and 'offset' fields
    val annotations = annotationsByPart.values.flatten.map(t => {
      Annotation(t._1, dataset.id, parts.find(_.title == t._2).get.id, t._3, None, None)
    })
    
    val allThings = parts.toSeq :+ parentThing
    AnnotatedThings.insertAll(allThings)
    Global.index.addAnnotatedThings(allThings)
    Global.index.refresh()
    Annotations.insertAll(annotations.toSeq)
    
    Logger.info("Import complete")
  }

  private def toMap(meta: Seq[String]): Map[String, String] = {
    val properties = meta.map(comment => comment.substring(1).split(":"))
    properties.foldLeft(Seq.empty[(String, String)])((map, prop) => {
      map :+ (prop(0).trim , prop(1).trim)
    }).toMap
  }

}
