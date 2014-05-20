package controllers.common.io

import java.util.UUID
import java.util.regex.Pattern
import models._
import play.api.db.slick._
import scala.io.Source

object CSVImporter extends BaseImporter {
  
  private val SEPARATOR = ";"
    
  private val SPLIT_REGEX = "(?<!\\\\)" + Pattern.quote(SEPARATOR)

  def importRecogitoCSV(source: Source, dataset: Dataset)(implicit s: Session) = {
    val data = source.getLines.toSeq
    
    val meta = toMap(data.takeWhile(_.startsWith("#")))  
    val header = data.drop(meta.size).take(1).toSeq.head.split(SEPARATOR, -1).toSeq
    
    // Recogito CSVs contain exactly one annotated thing
    val annotatedThing = AnnotatedThing(md5(dataset.id + " " + meta.get("title").get), 
      dataset.id, meta.get("title").get, None)
        
    AnnotatedThings.insert(annotatedThing)
    
    // TODO - should we re-use UUIDs out of Recogito?
    val annotations = data.drop(meta.size + 1).map(_.split(SPLIT_REGEX, -1)).map(fields =>
      Annotation(UUID.randomUUID(), dataset.id, annotatedThing.id, GazetteerURI(fields(header.indexOf("gazetteer_uri")))))
      
    Annotations.insert(annotations)
  }

  private def toMap(meta: Seq[String]): Map[String, String] = {
    val properties = meta.map(comment => comment.substring(1).split(":"))
    properties.foldLeft(Seq.empty[(String, String)])((map, prop) => {
      map :+ (prop(0).trim , prop(1).trim)
    }).toMap
  }

}