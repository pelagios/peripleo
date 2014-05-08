package controllers.common.io

import java.util.UUID
import java.util.regex.Pattern
import models._
import play.api.db.slick._
import scala.io.Source

class CSVImporter {
  
  private val SEPARATOR = ";"
    
  private val SPLIT_REGEX = "(?<!\\\\)" + Pattern.quote(SEPARATOR)

  def importRecogitoCSV(source: Source)(implicit s: Session) = {
    val data = source.getLines.toSeq
    
    val meta = toMap(data.takeWhile(_.startsWith("#")))  
    val header = data.drop(meta.size).take(1).toSeq.head.split(SEPARATOR, -1).toSeq
    
    AnnotatedThings.insert(AnnotatedThing(meta.get("title").get, meta.get("title").get, None))
    
    val annotations = data.drop(meta.size + 1).map(_.split(SPLIT_REGEX, -1)).map(fields =>
      Annotation(UUID.randomUUID, meta.get("title").get, fields(header.indexOf("gazetteer_uri"))))
    Annotations.insert(annotations)
  }

  private def toMap(meta: Seq[String]): Map[String, String] = {
    val properties = meta.map(comment => comment.substring(1).split(":"))
    properties.foldLeft(Seq.empty[(String, String)])((map, prop) => {
      map :+ (prop(0).trim , prop(1).trim)
    }).toMap
  }

}