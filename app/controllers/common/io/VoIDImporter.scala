package controllers.common.io

import java.io.FileInputStream
import java.sql.Date
import models.{ Dataset, Datasets }
import org.pelagios.Scalagios
import play.api.db.slick._
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.mvc.RequestHeader
import play.api.mvc.MultipartFormData.FilePart
import global.Global

object VoIDImporter extends AbstractImporter {
  
  def importVoID(file: FilePart[TemporaryFile], uri: Option[String] = None)(implicit s: Session, r: RequestHeader) = {
    Logger.info("Importing VoID file: " + file.filename)
    val format = getFormat(file.filename)
    
    // If we don't have a base URI for the VoID file, we'll use our own namespace as fallback
    // Not 100% the Sesame parser actually makes use of it... but we're keeping things sane nonetheless
    val baseURI = uri.getOrElse(controllers.routes.DatasetController.listAll().absoluteURL(false)(r))
    Scalagios.readVoID(new FileInputStream(file.ref.file), baseURI, format).foreach(dataset => {
      val id =
        if (dataset.uri.startsWith("http://")) {
          sha256(dataset.uri)          
        } else {
          sha256(dataset.title + " " + dataset.publisher)
        }
      
      Logger.info("Importing dataset '" + dataset.title + "' with ID " + id)
     
      val created = new Date(System.currentTimeMillis)
      val datasetEntity = Dataset(id, dataset.title, dataset.publisher, dataset.license,
        created, created, uri, dataset.description, dataset.homepage, 
        dataset.datadumps.headOption, None, None, None)
        
      Datasets.insert(datasetEntity)
      Global.index.addDataset(datasetEntity)
      Global.index.refresh()
    })
  }
  
}