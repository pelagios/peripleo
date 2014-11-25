package ingest

import global.Global
import java.io.FileInputStream
import java.sql.Date
import models.core.{ Dataset, Datasets }
import org.pelagios.Scalagios
import org.pelagios.api.dataset.{ Dataset => VoIDDataset }
import play.api.db.slick._
import play.api.Logger
import play.api.libs.Files.TemporaryFile

object VoIDImporter extends AbstractImporter {
  
  def readVoID(file: TemporaryFile, filename: String): Seq[VoIDDataset]= {
    Logger.info("Reading VoID file: " + filename)  
    val is = new FileInputStream(file.file)   
    val format = getFormat(filename)  
    val datasets = Scalagios.readVoID(is, format).toSeq
    is.close()
    datasets
  }
  
  def importVoID(file: TemporaryFile, filename: String, uri: Option[String] = None)(implicit s: Session): Seq[(Dataset, Seq[String])] =
    importVoID(readVoID(file, filename), uri)
  
  def importVoID(topLevelDatasets: Seq[VoIDDataset], uri: Option[String])(implicit s: Session): Seq[(Dataset, Seq[String])]= {
    // Helper to compute an ID for the dataset    
    def id(dataset: VoIDDataset) =
      if (dataset.uri.startsWith("http://")) {
        sha256(dataset.uri)          
      } else {
        sha256(dataset.title + " " + dataset.publisher)
      }

    // Helper to flatten the hierachy (of VoIDDatasets) into a list of (API) Datasets
    def flattenHierarchy(datasets: Seq[VoIDDataset], parent: Option[Dataset] = None): Seq[(Dataset, Seq[String])] = { 
      val created = new Date(System.currentTimeMillis)

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
          uri, d.description, d.homepage, parent.map(_.id), 
          None, None, None, None)
          
        (d, datasetEntity, d.datadumps)
      })
        
      datasetEntities.map(t => (t._2, t._3)) ++ datasetEntities.flatMap { case (d, entity, dumpfiles) 
        => flattenHierarchy(d.subsets, Some(entity)) }
    }
    
    val datasetsWithDumpfiles = flattenHierarchy(topLevelDatasets) 
    val datasets = datasetsWithDumpfiles.map(_._1)
    Datasets.insertAll(datasets)
    
    datasets.foreach(Global.index.addDataset(_))
    Global.index.refresh()
    
    datasetsWithDumpfiles
  }
  
}
