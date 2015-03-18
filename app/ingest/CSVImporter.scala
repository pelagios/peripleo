package ingest

import global.Global
import index.places.IndexedPlace
import java.util.UUID
import java.util.regex.Pattern
import models.Associations
import models.core._
import models.geo._
import play.api.Logger
import play.api.db.slick._
import scala.io.Source

object CSVImporter extends AbstractImporter {
  
  private val SEPARATOR = ";"
    
  private val SPLIT_REGEX = "(?<!\\\\)" + Pattern.quote(SEPARATOR)
  
  private def resolvePlaces(uris: Seq[String]): Seq[(IndexedPlace, Int)] = {
    val allReferencedPlaces = uris.distinct
      .map(uri => (uri, Global.index.findPlaceByURI(uri)))
      .filter(_._2.isDefined)
      .map(t => (t._1, t._2.get)).toMap
    
    uris.flatMap(uri => allReferencedPlaces.get(uri))
      .groupBy(_.uri)
      .map(t => (t._2.head, t._2.size))
      .toSeq
  }
  
  // TODO this only works on hierarchical docs now!
  def importRecogitoCSV(source: Source, dataset: Dataset)(implicit s: Session) = {
    val data = source.getLines.toSeq
    val meta = toMap(data.takeWhile(_.startsWith("#")))                   
    val header = data.drop(meta.size).take(1).toSeq.head.split(SEPARATOR, -1).toSeq
    val uuidIdx = header.indexOf("uuid")
    
    val annotations = data.drop(meta.size + 1).map(_.split(SPLIT_REGEX, -1)).map(fields => {
      val uuid = if (uuidIdx > -1) UUID.fromString(fields(uuidIdx)) else UUID.randomUUID 
      val gazetteerURI = fields(header.indexOf("gazetteer_uri"))
      val toponym = fields(header.indexOf("toponym"))
      
      // In case annotations are on the root thing, the document part is an empty string!
      val documentPart = fields(header.indexOf("document_part")) 
      (uuid, documentPart, gazetteerURI, toponym)     
    }).groupBy(_._2)

    val annotationsOnRoot = annotations.filter(_._1.isEmpty).toSeq.flatMap(_._2.map(t => (t._1, t._3, t._4)))
    val annotationsForParts = annotations.filter(!_._1.isEmpty)
    
    // For CSVs, 'fulltext' is simply a concatenation of all distinct toponyms in the table
    val fulltextOnRoot = {
      val fulltext = annotationsOnRoot.map(_._3).distinct.mkString(" ")
      if (fulltext.isEmpty)
        None
      else
        Some(fulltext)
    }
    val fulltextForParts = annotationsForParts.mapValues(t => t.map(_._4).mkString(" "))
    
    val ingestBatch = {
      // Root thing
      val rootTitle = meta.get("author").map(_ + ": ").getOrElse("") + meta.get("title").get + meta.get("language").map(" (" + _ + ")").getOrElse("")
      val rootThingId = sha256(dataset.id + " " + meta.get("author").getOrElse("") + rootTitle + " " + meta.get("language").getOrElse(""))
      val date = meta.get("date (numeric)").map(_.toInt)
      
      val partIngestBatch = annotationsForParts.map { case (partTitle, tuples) =>
        val partThingId = sha256(rootThingId + " " + partTitle)
        
        val annotations = tuples.zipWithIndex.map { case ((uuid, _, gazetteerURI, toponym), index) => 
          Annotation(uuid, dataset.id, partThingId, gazetteerURI, Some(toponym), Some(index)) }
        
        val places = 
          resolvePlaces(annotations.map(_.gazetteerURI))

        val thing = 
          AnnotatedThing(partThingId, dataset.id, partTitle, None, Some(rootThingId), None, date, date, ConvexHull.fromPlaces(places.map(_._1)))
          
        IngestRecord(thing, annotations, places, fulltextForParts.get(partTitle), Seq.empty[Image])
      }.toSeq
     
      // Root thing
      val rootAnnotations = annotationsOnRoot.zipWithIndex.map { case ((uuid, gazetteerURI, toponym), index) => Annotation(uuid, dataset.id, rootThingId, gazetteerURI, Some(toponym), Some(index)) }
      
      // The places of the root thing consist of the places *directly* on the root thing and the places on all parts
      // Usually, only one list will be non-empty - but we add them, just in case
      val rootPlaces = resolvePlaces(rootAnnotations.map(_.gazetteerURI))
      val partPlaces = resolvePlaces(partIngestBatch.flatMap(_.annotations).map(_.gazetteerURI))
      val allPlaces = (rootPlaces ++ partPlaces).groupBy(_._1).foldLeft(Seq.empty[(IndexedPlace, Int)]){ case (result, (place, list)) =>
        result :+ (place, list.map(_._2).sum) }
      
      val rootThing = AnnotatedThing(rootThingId, dataset.id, rootTitle, None, None, None, date, date, ConvexHull.fromPlaces(allPlaces.map(_._1)))
      
      IngestRecord(rootThing, rootAnnotations, allPlaces, fulltextOnRoot, Seq.empty[Image]) +: partIngestBatch
    }

    // Insert data into DB
    ingest(ingestBatch, dataset)
    
    source.close()
    Logger.info("Import complete")    
  }

  private def toMap(meta: Seq[String]): Map[String, String] = {
    val properties = meta.map(comment => comment.substring(1).split(":"))
    properties.foldLeft(Seq.empty[(String, String)])((map, prop) => {
      map :+ (prop(0).trim , prop(1).trim)
    }).toMap
  }

}
