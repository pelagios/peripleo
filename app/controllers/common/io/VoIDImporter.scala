package controllers.common.io

import java.io.FileInputStream
import java.sql.Date
import models.{ Dataset, Datasets }
import org.pelagios.Scalagios
import org.pelagios.api.dataset.{ Dataset => VoidDataset }
import play.api.db.slick._
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.mvc.RequestHeader
import play.api.mvc.MultipartFormData.FilePart
import global.Global

object VoIDImporter extends AbstractImporter {
  
  def importVoID(file: FilePart[TemporaryFile], uri: Option[String] = None)(implicit s: Session, r: RequestHeader) = {
    Logger.info("Importing VoID file: " + file.filename)  
    val is = new FileInputStream(file.ref.file)   
    val format = getFormat(file.filename)  
    val created = new Date(System.currentTimeMillis)
    
    def id(dataset: VoidDataset) =
      if (dataset.uri.startsWith("http://")) {
        sha256(dataset.uri)          
      } else {
        sha256(dataset.title + " " + dataset.publisher)
      }

    def flattenHierarchy(datasets: Seq[VoidDataset], parentId: Option[String] = None): Seq[Dataset] = {      
      val datasetEntities = datasets.map(d => {
        Dataset(id(d), d.title, d.publisher, d.license, created, created, 
          uri, d.description, d.homepage, parentId, d.datadumps.headOption, 
          None, None, None)
      })
        
      datasetEntities ++ datasets.flatMap(d => flattenHierarchy(d.subsets, Some(id(d))))
    }
    
    val datasets = flattenHierarchy(Scalagios.readVoID(is, format).toSeq) 
    Datasets.insertAll(datasets)
    datasets.foreach(Global.index.addDataset(_))
    Global.index.refresh()
    
    is.close()
  }
  
}