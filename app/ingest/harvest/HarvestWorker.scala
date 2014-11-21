package ingest.harvest

import java.net.URL
import java.io.File
import models.Associations
import models.core._
import global.Global
import play.api.Play.current
import play.api.db.slick._
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import sys.process._
import scala.io.Source
import ingest.CSVImporter
import ingest.PelagiosOAImporter
import java.util.UUID
import org.apache.commons.codec.digest.DigestUtils
import java.io.FileInputStream
import ingest.VoIDImporter
import org.pelagios.api.dataset.{ Dataset => VoIDDataset }

class HarvestWorker {
  
  private val TMP_DIR = System.getProperty("java.io.tmpdir")
  private val UTF8 = "UTF-8"
  private val CSV = "csv"
    
  /** Helper to compute the hash of a file **/
  private def computeHash(file: File): String = {
    /*
    val is = new FileInputStream(file)
    val hash = DigestUtils.md5Hex(is)
    is.close()
    hash
    */
    ""
  }
  
  /** Helper to compute a hash for multiple files **/
  private def computeHash(files: Seq[File]): String = {
    ""
  }
  
  /** Helper to get datadump URLs for a dataset and all its subsets **/
  private def getDatadumpURLs(dataset: VoIDDataset): Seq[String] = {
    if (dataset.subsets.isEmpty) {
      dataset.datadumps
    } else {
      dataset.datadumps ++ dataset.subsets.flatMap(getDatadumpURLs(_))
    }
  }
  
  /** Helper to drop a dataset and all its dependencies from DB and index 
    *
    * TODO there is now code duplication with the DatasetAdminController - resolve!
    */
  private def dropDatasetCascaded(id: String)(implicit s: Session) = {
    val subsetsRecursive = id +: Datasets.listSubsetsRecursive(id)

    // Purge from database
    Annotations.deleteForDatasets(subsetsRecursive)
    Associations.deleteForDatasets(subsetsRecursive)
    Images.deleteForDatasets(subsetsRecursive)
    AnnotatedThings.deleteForDatasets(subsetsRecursive)
    DatasetDumpfiles.deleteForDatasets(subsetsRecursive)
    Datasets.delete(subsetsRecursive)
    
    // Purge from index
    Global.index.dropDatasets(subsetsRecursive)
    Global.index.refresh()    
  }
      
  /** (Re-)Harvest a dataset from a VoID URL **/
  def fullHarvest(voidURL: String, previous: Option[Dataset]) = {	  
    Logger.info("Downloading VoID from " + voidURL)
    val startTime = System.currentTimeMillis
   
    // Assign a random (but unique) name, and keep the extension from the original file
    val voidFilename = "void_" + UUID.randomUUID.toString + voidURL.substring(voidURL.lastIndexOf("."))
    val voidTempFile = new TemporaryFile(new File(TMP_DIR, voidFilename))
 
    try {
      // Download
	  new URL(voidURL) #> voidTempFile.file !!
	
	  Logger.info("Download complete from " + voidURL)
    
	  val voidHash = computeHash(voidTempFile.file)
	
	  // We only support one top-level dataset per VoID
	  val dataset = VoIDImporter.readVoID(voidTempFile, voidFilename).head
	  voidTempFile.finalize()
	
	  val dataDumpURLs = getDatadumpURLs(dataset)
	  val dataDumps = dataDumpURLs.par.map(url => {
	    Logger.info("Downloading datadump from " + url)
	    val dumpFilename = "data_" + UUID.randomUUID.toString + url.substring(url.lastIndexOf("."))
	    val dumpTempFile = new TemporaryFile(new File(TMP_DIR, dumpFilename))
	    
	    new URL(url) #> dumpTempFile.file !!
	      
	    Logger.info("Download complete from " + url)
	   
	    dumpTempFile
	  }).seq
	  Logger.info("All downloads complete for VoID " + voidURL)
	
	  val dataHash = computeHash(dataDumps.map(_.file))
	  	
	  // TODO compare hashes and only ingest on change
	
	  DB.withSession { implicit session: Session =>
	    // Drop
	    if (previous.isDefined)
	      dropDatasetCascaded(previous.get.id)

	    // Import
	    Logger.info("Importing dataset " + dataset.title)
	    val importedDataset = VoIDImporter.importVoID(Seq(dataset), Some(voidURL)).head
	    Logger.info("Import complete for dataset " + dataset.title)
	  
	    dataDumps.foreach(dump => {
	      Logger.info("Importing " + dump.file.getName)
	      if (dump.file.getName.endsWith(CSV))
            CSVImporter.importRecogitoCSV(Source.fromFile(dump.file, UTF8), importedDataset)
          else
            PelagiosOAImporter.importPelagiosAnnotations(dump, dump.file.getName, importedDataset)
           
          dump.finalize()
          Logger.info(dump.file.getName + " - import complete.")
	    })
  	  }
    } catch {
      // TODO retry?
      case t: Throwable => Logger.info(t.getMessage())
    }
  }
	
  def harvest(datasetId: String) = {
    DB.withSession { implicit session: Session =>
      val d = Datasets.findByIdWithDumpfiles(datasetId)
      if (d.isEmpty) {
	    // TODO error notification
      } else {
	    val (dataset, dumpfileURLs) = d.get
 	    val dumpfiles = dumpfileURLs.par.map(d => { 
	      val filename = d.uri.substring(d.uri.lastIndexOf("/") + 1)
	      Logger.info("Downloading file " + filename + " from " + d.uri)
	      val tempFile = new TemporaryFile(new File(TMP_DIR, filename))
	      new URL(d.uri) #> tempFile.file !!
	      
	      Logger.info(filename + " - download complete.")
	      tempFile
	    }).seq
	    Logger.info("All downloads complete.")
	    
	    dumpfiles.foreach(file => {
	      Logger.info("Importing " + file.file.getName)
	      if (file.file.getName.endsWith(CSV))
            CSVImporter.importRecogitoCSV(Source.fromFile(file.file, UTF8), dataset)
          else
            PelagiosOAImporter.importPelagiosAnnotations(file, file.file.getName, dataset)
           
          file.finalize()
          Logger.info(file.file.getName + " - import complete.")
	    })
	    Logger.info("All downloads imported.")
     }
    }
  }
	
}
