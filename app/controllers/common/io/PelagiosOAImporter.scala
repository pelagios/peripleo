package controllers.common.io

import java.util.UUID
import java.io.FileInputStream
import models._
import play.api.Logger
import play.api.db.slick._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.RequestHeader
import play.api.mvc.MultipartFormData.FilePart
import org.openrdf.rio.RDFFormat
import org.pelagios.Scalagios

object PelagiosOAImporter extends AbstractImporter {

  def importPelagiosAnnotations(file: FilePart[TemporaryFile], dataset: Dataset)(implicit s: Session, r: RequestHeader) = {
    Logger.info("Reading Pelagios annotations from RDF: " + file.filename) 
    val format = getFormat(file.filename)
    
    // If we don't have a base URI for the VoID file, we'll use our own namespace as fallback
    // Not 100% the Sesame parser actually makes use of it... but we're keeping things sane nonetheless
    val baseURI = controllers.routes.DatasetController.listAll(None, None, None).absoluteURL(false)(r)
    val annotatedThings = Scalagios.readAnnotations(new FileInputStream(file.ref.file), baseURI, format)
    Logger.info("Importing " + annotatedThings.size + " annotated things with " + annotatedThings.flatMap(_.annotations).size + " annotations")
    
    val annotations = annotatedThings.flatMap(thing => {
      
      // TODO add support for annotated things with multi-level hierarchy
      
      val thingId = md5(thing.uri)
      AnnotatedThings.insert(AnnotatedThing(thingId, dataset.id, thing.title, None))
     
      thing.annotations.map(a => Annotation(UUID.randomUUID, dataset.id, thingId, new GazetteerURI(a.place.head)))
    })
    
    Annotations.insert(annotations.toSeq)
  }
  
}