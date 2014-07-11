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

    def flattenHierarchy(datasets: Seq[VoidDataset], parent: Option[Dataset] = None): Seq[Dataset] = { 
      val datasetEntities = datasets.map(d => {
        val publisher =
          if (d.publisher.isDefined)
            d.publisher.get
          else
            parent.map(_.publisher).getOrElse("[NO PUBLISHER]")  
        
        val license =
          if (d.license.isDefined)
            d.license.get 
          else 
            parent.map(_.license).getOrElse("[NO LICENSE]")
        
        val datasetEntity = Dataset(id(d), d.title, publisher, license, created, created, 
          uri, d.description, d.homepage, parent.map(_.id), d.datadumps.headOption, 
          None, None, None)
          
        (d, datasetEntity)
      })
        
      datasetEntities.map(_._2) ++ datasetEntities.flatMap { case (void, entity) 
        => flattenHierarchy(void.subsets, Some(entity)) }
    }
    
    val datasets = flattenHierarchy(Scalagios.readVoID(is, format).toSeq) 
    Datasets.insertAll(datasets)
    datasets.foreach(Global.index.addDataset(_))
    Global.index.refresh()
    
    is.close()
  }
  
}