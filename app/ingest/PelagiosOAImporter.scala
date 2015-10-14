package ingest

import global.Global
import index.Index
import index.places.IndexedPlaceNetwork
import java.util.{ Calendar, Date, UUID }
import java.io.FileInputStream
import models.Associations
import models.core._
import models.geo.Hull
import org.pelagios.Scalagios
import org.pelagios.api.annotation.{ AnnotatedThing => OAThing, Annotation => OAnnotation }
import play.api.Logger
import play.api.Play
import play.api.db.slick._
import play.api.libs.Files.TemporaryFile

object PelagiosOAImporter extends AbstractImporter {
  
  /** The maximum number of AnnotatedThings to ingest in one batch **/
  private val BATCH_SIZE = {
    val batchsize = Play.current.configuration.getString("peripleo.ingest.batchsize").getOrElse("30000").trim().toInt
    Logger.info("Open Annotation import batch size set to " + batchsize)
    batchsize
  }
  
  /** Given a thing, this function returns a list of all things below it in the hierarchy **/
  private def flattenThingHierarchy(thing: OAThing): Seq[OAThing] =
    if (thing.parts.isEmpty)
      thing.parts
    else
      thing.parts ++ thing.parts.flatMap(flattenThingHierarchy(_))
        
  /** Returns all annotations below an annotated thing, recurses down the thing hierarchy **/
  private def getAnnotationsRecursive(thing: OAThing): Seq[OAnnotation] =
    if (thing.parts.isEmpty)
      thing.annotations
    else
      thing.annotations ++ thing.parts.flatMap(getAnnotationsRecursive(_))
  
  /** Resolves the places referenced by a Seq of annotations.
    *
    * @returns a maps (indexedPlace -> number of times referenced)  
    */
  private def resolvePlaces(annotations: Seq[OAnnotation]): Seq[(IndexedPlaceNetwork, String, Int)] = { 
    // Resolve all gazetteer URIs that occur in the annotations against index
    val allReferencedPlaces = annotations.flatMap(_.places).distinct
      .map(uri => (uri, Global.index.findPlaceByAnyURI(uri)))
      .filter(_._2.isDefined)
      .map(t => (t._1, t._2.get)).toMap
          
    // Ok - this is a little complicated. Background: every annotation can come with multiple gazetteer
    // URIs. These might point to the same place (e.g. one Pleiades URI, one equivalent GeoNames URI).
    // But that doesn't have to be the case! It's also valid for an annotation to point to multiple places.
    // We want to remove the duplicates, but keep the intentional multi-references.
    //
    // This operation creates a list of place-networks in the index each annotation refers to, de-duplicates
    // the list, and then keeps the first URI used by the annotations. (Savvy?)
    val referencedPlacesWithoutDuplicates = annotations.par.flatMap(_.places
          .map(uri => (allReferencedPlaces.get(uri), uri))        
          .filter(_._1.isDefined)
          .map(t => (t._1.get, t._2))
          .groupBy(_._1.seedURI)
          .map(_._2.head)
        ).seq
        
    referencedPlacesWithoutDuplicates.groupBy(_._1.seedURI).map { case (seedURI, places) => (places.head._1, places.head._2, places.size) }.toSeq
  }
    
  
  def importPelagiosAnnotations(file: TemporaryFile, filename: String, dataset: Dataset)(implicit s: Session) = {
    Logger.info("Reading Pelagios annotations from RDF: " + filename) 
    val is = new FileInputStream(file.file)
    val annotatedThings = Scalagios.readAnnotations(is, filename)
    Logger.info("Importing " + annotatedThings.size + " annotated things with " + annotatedThings.flatMap(_.annotations).size + " annotations")
    
    annotatedThings.grouped(BATCH_SIZE).foreach(batch => {
      importBatch(batch, dataset)
      Logger.info("Importing next batch")      
    })
        
    is.close()
    Logger.info("Import of " + filename + " complete")
  }
  
  private def importBatch(annotatedThings: Iterable[OAThing], dataset: Dataset)(implicit s: Session) = {
    // Flatten the things, so that we have a list of all things in the hierarchy tree. Then, for
    // each thing, get all annotations and resolve the places referenced by them
    val preparedForIngest = annotatedThings.flatMap(rootThing => {
      val flattendHierarchy = rootThing +: flattenThingHierarchy(rootThing)
      flattendHierarchy.map(thing => {
        val annotations = getAnnotationsRecursive(thing) 
        val places = resolvePlaces(annotations)
        if (places.flatMap(_._1.geometry).size > 0) {
          Some((thing, rootThing, places))
        } else {
          Logger.warn("Discarding item with 0 resolvable geometries:")
          Logger.warn(thing.title)
          annotations.foreach(a => Logger.warn(a.places.mkString(", ")))
          None
        }
      })      
    }).toSeq.flatten
    
    // Ingest
    val ingestBatch = preparedForIngest.map { case (oaThing, oaRootThing, places) => { 
      val thingId = sha256(oaThing.uri)
      
      val tempBoundsStart = oaThing.temporal.map(period => period.startYear)
      val tempBoundsEnd = if (tempBoundsStart.isDefined) {
        val periodEnd = oaThing.temporal.flatMap(_.endYear)
        if (periodEnd.isDefined)
          periodEnd
        else
          tempBoundsStart // Repeat start date in case no end is defined  
      } else {
        None
      }
      
      val thing = 
        AnnotatedThing(thingId,
                       dataset.id,
                       oaThing.title,
                       oaThing.description,
                       oaThing.isPartOf.map(parent => sha256(parent.uri)),
                       oaThing.homepage, 
                       tempBoundsStart, 
                       tempBoundsEnd, 
                       Hull.fromPlaces(places.map(_._1)))
      
      val images = 
        oaThing.depictions.map(url => Image(None, dataset.id, thingId, url))
      
      // TODO make use of 'quote' and 'offset' fields
      val annotations = oaThing.annotations.map(a =>
        Annotation(UUID.randomUUID, dataset.id, thingId, a.places.head, None, None))     
        
      IngestRecord(thing, annotations.map((_, None, None)), places, None, images)
    }}
      
    // Insert data into DB
    ingest(ingestBatch, dataset)
  }
  
}
