package ingest

import global.Global
import index.Index
import index.places.IndexedPlaceNetwork
import java.math.BigInteger
import java.security.MessageDigest
import models.Associations
import models.adjacency.{ PlaceAdjacency, PlaceAdjacencys }
import models.core._
import models.geo.GazetteerReference
import org.pelagios.Scalagios
import play.api.db.slick._
import play.api.libs.json.Json
import play.api.Logger

/** One 'ingest record' **/
case class IngestRecord(

    /** The annotated thing **/
    thing: AnnotatedThing, 
    
    /** Annotations on the annotated thing **/
    annotationsWithText: Seq[(Annotation, Option[String], Option[String])],
    
    /** Places associated with the annotated thing, with place count **/
    places: Seq[(IndexedPlaceNetwork, String, Int)],

    /** Fulltext connected to the thing, if any **/
    fulltext: Option[String],

    /** Images related to the annotated thing **/
    images: Seq[Image]
    
)

abstract class AbstractImporter {
  
  private val SHA256 = "SHA-256"
    
  private val UTF8 = "UTF-8"
    
  private def computePlaceAdjacency(thingId: String, annotations: Seq[Annotation], places: Map[String, IndexedPlaceNetwork]): Seq[PlaceAdjacency] = {
    // Pairs of adjacent annotations (i.e. those that follow in the list)
    val annotationAdjacencyPairs = annotations.sliding(2).toSeq
    
    // Now we group our pairs by (place, adjacentPlace)
    annotationAdjacencyPairs
      .groupBy(pair => (pair.head.gazetteerURI, pair.last.gazetteerURI)).toSeq
      .map { case ((placeURI, nextPlaceURI), pairs) => {
        val place = places.get(placeURI)
        val nextPlace = places.get(nextPlaceURI)
        
        if (place.isDefined && nextPlace.isDefined && place.map(_.seedURI) != nextPlace.map(_.seedURI)) {
          Some(PlaceAdjacency(
            None, 
            pairs.head.head.annotatedThing,
            place.map(p => GazetteerReference(p.seedURI, p.title, p.geometryJson)).get,
            nextPlace.map(p => GazetteerReference(p.seedURI, p.title, p.geometryJson)).get,
            pairs.size
          ))
        } else {
          None
        }
      }}.flatten
  }
  
  private def getRoot(thingId: String, parentTable: Map[String, String]): String = 
    parentTable.get(thingId) match {
      case Some(parentId) => getRoot(parentId, parentTable)
      case None => thingId
    }
  
  private def buildFullText(parent: IngestRecord, ingestBatch: Seq[IngestRecord]): Seq[String] = {
    val children = ingestBatch.filter(_.thing.isPartOf == Some(parent.thing.id))
    (parent.fulltext +: children.map(_.fulltext)).flatten ++ children.flatMap(r => buildFullText(r, ingestBatch))
  }
   
  protected def ingest(ingestBatch: Seq[IngestRecord], dataset: Dataset)(implicit s: Session) {
    // Insert data into DB
    val allThings = ingestBatch.map(_.thing)
    AnnotatedThings.insertAll(allThings)

    val allImages = ingestBatch.flatMap(_.images)
    Images.insertAll(allImages)

    val allAnnotations = ingestBatch.flatMap(_.annotationsWithText.map(_._1))
    Annotations.insertAll(allAnnotations)
       
    // Lookup table for places by ID
    val placeLookup = ingestBatch.flatMap(record => record.places.map(p => (p._2, p._1))).toMap
    
    // Lookup table for things' parent IDs 
    val parentTable = ingestBatch.map(record => (record.thing.id, record.thing.isPartOf))
      .filter(_._2.isDefined)
      .toMap
      .mapValues(_.get)
    
    // Update aggregation table stats
    Associations.insert(ingestBatch.map(record => (record.thing, record.places)))
    
    // Place adjacency (only for annotated things with 2+ annotations!)
    val allAdjacencies = 
      ingestBatch.filter(_.annotationsWithText.size > 1)
        .flatMap(record => computePlaceAdjacency(record.thing.id, record.annotationsWithText.map(_._1), placeLookup))
      
    PlaceAdjacencys.insertAll(allAdjacencies)
    
    // Update the parent dataset with new temporal profile and convex hull
    val affectedDatasets = Datasets.recomputeSpaceTimeBounds(dataset)
    
    // Update object index - note: we only index metadata for top-level items, but want fulltext from children as well
    Logger.info("Updating Index") 
    val topLevelThings = ingestBatch.filter(_.thing.isPartOf.isEmpty).map(r => {
      val collapsedFulltext = {        
        // Note: this should slightly speed up ingest of datasets with large no. of items & no text
        val childTexts = buildFullText(r, ingestBatch.filter(_.fulltext.isDefined))
        if (childTexts.isEmpty)
          None
        else
          Some(childTexts.mkString(" "))
      }
      
      (r.thing, r.places.map(t => (t._1, t._2)), r.images, collapsedFulltext)
    })
    
    val datasetHierarchy = dataset +: Datasets.getParentHierarchyWithDatasets(dataset)
    Logger.info("Indexing items")
    Global.index.addAnnotatedThings(topLevelThings, datasetHierarchy)
    Global.index.updateDatasets(affectedDatasets)
    
    // Update annotation index
    val annotationsWithContext = ingestBatch.flatMap(record => {
      // Temporal bounds of the annotation are those of their annotated thing
      val thing = record.thing
      
      // Title and description are that of the root thing
      val rootThing = allThings.find(_.id == getRoot(thing.id, parentTable)).getOrElse(thing)
      
      record.annotationsWithText.map { case (annotation, prefix, suffix) => {        
        // The annotation index is to support spatial vis, so we're not interested in annotations without geometry
        val place = placeLookup.get(annotation.gazetteerURI)
        if (place.flatMap(_.geometry).isDefined)
          place.map(p => (rootThing, thing, annotation, p, prefix, suffix))
        else
          None
      }}
    }).flatten
    Logger.info("Indexing " + annotationsWithContext.size + " annotations")
    Global.index.addAnnotations(annotationsWithContext)
    
    // Update suggestion index
    Logger.info("Updating the suggestion index")
    val titlesAndDescriptions = topLevelThings.flatMap(t => Seq(Some(t._1.title), t._1.description).flatten).distinct
    val fulltext = topLevelThings.map(_._4).flatten
    Global.index.suggester.addTerms(titlesAndDescriptions ++ fulltext)
    Global.index.refresh()
  }
    
  /** Utility method that returns the RDF format corresponding to a particular file extension **/
  protected def getFormat(filename: String): String = filename match {
    case f if f.endsWith("rdf") => Scalagios.RDFXML
    case f if f.endsWith("ttl") => Scalagios.TURTLE
    case f if f.endsWith("n3") => Scalagios.N3
    case _ => throw new IllegalArgumentException("Format not supported")
  }
  
  /** Utility method that produces a SHA256 hash from a string **/
  protected def sha256(str: String): String = {
    val md = MessageDigest.getInstance(SHA256).digest(str.getBytes(UTF8))
    (new BigInteger(1, md)).toString(16)
  }

}