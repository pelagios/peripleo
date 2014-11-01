package ingest

import global.Global
import index.places.IndexedPlace
import java.util.UUID
import java.io.FileInputStream
import models.Associations
import models.core._
import models.geo.ConvexHull
import org.pelagios.Scalagios
import org.pelagios.api.annotation.{ AnnotatedThing => OAThing, Annotation => OAnnotation }
import play.api.Logger
import play.api.db.slick._
import play.api.libs.Files.TemporaryFile

object PelagiosOAImporter extends AbstractImporter {
  
  /** The maximum number of AnnotatedThings to ingest in one batch **/
  private val BATCH_SIZE = 30000
  
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
  private def resolvePlaces(annotations: Seq[OAnnotation]): Seq[(IndexedPlace, Int)] = { 
    // Resolve all gazetteer URIs that occur in the annotations against index
    val allReferencedPlaces = annotations.flatMap(_.places).distinct
      .map(uri => (uri, Global.index.findNetworkByPlaceURI(uri)))
      .filter(_._2.isDefined)
      .map(t => (t._1, t._2.get)).toMap
    
    // Ok - this is a little complicated. Background: every annotation can come with multiple gazetteer
    // URIs. These might point to the same place (e.g. one Pleiades URI, one equivalent GeoNames URI).
    // But that doesn't have to be the case! It's also valid for an annotation to point to multiple places.
    // We want to remove the duplicates, but keep the intentional multi-references.
    //
    // This operation creates a list of place-networks in the index the annotations refer to, de-duplicates
    // the list, and then keeps the place from the network that was referenced by the (first) URI in the 
    // annotation. (Savvy?)
    val referencedPlacesWithoutDuplicates = annotations.par.flatMap(_.places
        .map(uri => (uri, allReferencedPlaces.get(uri)))        
        .filter(_._2.isDefined)
        .map(t => (t._1, t._2.get))
        .groupBy(_._2.seedURI)
        .map(_._2.head)
        .map { case (originalURI, network) => network.getPlace(originalURI).get }).seq
        
    referencedPlacesWithoutDuplicates.groupBy(_.uri).map { case (uri, places) => (places.head, places.size) }.toSeq 
  }
    
  
  def importPelagiosAnnotations(file: TemporaryFile, filename: String, dataset: Dataset)(implicit s: Session) = {
    Logger.info("Reading Pelagios annotations from RDF: " + filename) 
    val format = getFormat(filename)
    
    val is = new FileInputStream(file.file)
    val annotatedThings = Scalagios.readAnnotations(is, format)
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
        (thing, resolvePlaces(annotations))
      })      
    }).toSeq
    
    // Ingest
    val ingestBatch = preparedForIngest.map { case (oaThing, places) => { 
      val thingId = sha256(oaThing.uri)
      
      val tempBoundsStart = oaThing.temporal.map(_.start)
      val tempBoundsEnd = if (tempBoundsStart.isDefined) {
        val periodEnd = oaThing.temporal.flatMap(_.end)
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
                       ConvexHull.fromPlaces(places.map(_._1)))
      
      val images = 
        oaThing.depictions.map(url => Image(None, dataset.id, thingId, url, false)) ++
        oaThing.thumbnails.map(url => Image(None, dataset.id, thingId, url, true))
        
      // TODO make use of 'quote' and 'offset' fields
      val annotations = oaThing.annotations.map(a =>
        Annotation(UUID.randomUUID, dataset.id, thingId, a.places.head, None, None))     
        
      IngestRecord(thing, images, annotations, places)
    }}
      
    // Insert data into DB
    ingest(ingestBatch, dataset)
  }
  
}
