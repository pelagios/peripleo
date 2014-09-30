package controllers.common.io

import models.TemporalProfile
import global.Global
import java.util.{ Calendar, UUID }
import java.io.FileInputStream
import java.sql.Date
import models._
import play.api.Logger
import play.api.db.slick._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.RequestHeader
import play.api.mvc.MultipartFormData.FilePart
import org.openrdf.rio.RDFFormat
import org.pelagios.Scalagios

object PelagiosOAImporter extends AbstractImporter {

  def importPelagiosAnnotations(file: TemporaryFile, filename: String, dataset: Dataset)(implicit s: Session) = {
    Logger.info("Reading Pelagios annotations from RDF: " + filename) 
    val format = getFormat(filename)
    
    val is = new FileInputStream(file.file)
    val annotatedThings = Scalagios.readAnnotations(is, format)
    Logger.info("Importing " + annotatedThings.size + " annotated things with " + annotatedThings.flatMap(_.annotations).size + " annotations")
    
    // Parse data
    val ingestBatch: Seq[(AnnotatedThing, Seq[Annotation])] = annotatedThings.toSeq.map(oaThing => { 
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
      
      val thing = AnnotatedThing(thingId, dataset.id, oaThing.title, None, oaThing.homepage, tempBoundsStart, tempBoundsEnd)
      
      // TODO make use of 'quote' and 'offset' fields
      val annotations = oaThing.annotations.map(a =>
        Annotation(UUID.randomUUID, dataset.id, thingId, a.place.head, None, None))     
        
      (thing, annotations)
    })
      
    // Insert data into DB and index
    val allThings = ingestBatch.map(_._1)
    val allAnnotations = ingestBatch.flatMap(_._2)
    AnnotatedThings.insertAll(allThings)
    
    Global.index.addAnnotatedThings(allThings)
    Global.index.refresh()
    
    Annotations.insertAll(allAnnotations)
    
    // Update the parent dataset with new temporal bounds and profile
    val datedThings = allThings.filter(_.temporalBoundsStart.isDefined)
    
    val (tempBoundsStart, tempBoundsEnd, temporalProfile) = if (datedThings.isEmpty)
      (None, None, None) 
    else
      (Some(datedThings.map(_.temporalBoundsStart.get).min),
       Some(datedThings.map(_.temporalBoundsEnd.get).max),
       Some(new TemporalProfile(datedThings.map(thing => (thing.temporalBoundsStart.get, thing.temporalBoundsEnd.get))).toString))  
        
    // TODO insert dataset dumpfiles
    
    val updatedDataset = Dataset(dataset.id, dataset.title, dataset.publisher, dataset.license,
        dataset.created, new Date(System.currentTimeMillis), dataset.voidURI, dataset.description, 
        dataset.homepage, None, tempBoundsStart, tempBoundsEnd, temporalProfile)
        
    Datasets.update(updatedDataset) 
    
    is.close()
    
    Logger.info("Import of " + filename + " complete")
  }
  
}
