package ingest

import global.Global
import java.io.FileInputStream
import java.sql.Date
import models.core.{ Dataset, Datasets, DatasetDumpfile, DatasetDumpfiles }
import org.pelagios.Scalagios
import org.pelagios.api.dataset.{ Dataset => VoidDataset }
import play.api.db.slick._
import play.api.Logger
import play.api.libs.Files.TemporaryFile

object VoIDImporter extends AbstractImporter {
  
  def importVoID(file: TemporaryFile, filename: String, uri: Option[String] = None)(implicit s: Session) = {
    Logger.info("Importing VoID file: " + filename)  
    val is = new FileInputStream(file.file)   
    val format = getFormat(filename)  
    val created = new Date(System.currentTimeMillis)
    
    def id(dataset: VoidDataset) =
      if (dataset.uri.startsWith("http://")) {
        sha256(dataset.uri)          
      } else {
        sha256(dataset.title + " " + dataset.publisher)
      }

    def flattenHierarchy(datasets: Seq[VoidDataset], parent: Option[Dataset] = None): Seq[(Dataset, Seq[DatasetDumpfile])] = { 
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
          
        val dumpfiles = d.datadumps.map(uri => DatasetDumpfile(uri, id(d), None))
  
        (d, datasetEntity, dumpfiles)
      })
        
      datasetEntities.map(t => (t._2, t._3)) ++ datasetEntities.flatMap { case (d, entity, dumpfiles) 
        => flattenHierarchy(d.subsets, Some(entity)) }
    }
    
    val datasets = flattenHierarchy(Scalagios.readVoID(is, format).toSeq) 
    Datasets.insertAll(datasets.map(_._1))
    DatasetDumpfiles.insertAll(datasets.flatMap(_._2))
    
    datasets.foreach(t => Global.index.addDataset(t._1))
    Global.index.refresh()
    
    is.close()
  }
  
}
