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
import java.security.MessageDigest
import java.math.BigInteger

class HarvestWorker {
  
  private val TMP_DIR = System.getProperty("java.io.tmpdir")
  
  private val UTF8 = "UTF-8"
    
  private val CSV = "csv"
  
  private val MD5 = "MD5"
    
  private val MAX_RETRIES = 5
  
  /** Helper to download a file from a URL **/
  private def downloadFile(url: String, filename: String, failedAttempts: Int = 0): Option[TemporaryFile] = {
    try {
      Logger.info("Downloading " + url)
      val tempFile = new TemporaryFile(new File(TMP_DIR, filename))
	    new URL(url) #> tempFile.file !!
	  
	    Logger.info("Download complete for " + url)
	    Some(tempFile)
    } catch {
      case t: Throwable => {
        if (failedAttempts < MAX_RETRIES) {
          Logger.info("Download failed - retrying " + url)
          downloadFile(url, filename, failedAttempts + 1)
        } else {
          Logger.info("Download failed " + failedAttempts + " - giving up")
          None
        }
      }
    }    
  }
  
  /** Helper to compute the hash of a file **/
  private def computeHash(file: File): String = computeHash(Seq(file))
  
  /** Helper to compute a hash for multiple files **/
  private def computeHash(files: Seq[File]): String = {
    val md = MessageDigest.getInstance(MD5)
    files.foreach(file => {
      val is = new FileInputStream(file);
      Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).grouped(1024).foreach(bytes => {
        md.update(bytes.toArray, 0, bytes.size)
      })
    })
    val mdBytes = md.digest()
    new BigInteger(1, mdBytes).toString(16)
  }
  
  /** Helper to get datadump URLs for a dataset and all its subsets **/
  private def getDataDumpURLs(datasets: Seq[VoIDDataset]): Seq[(String, VoIDDataset)] = {
    datasets.flatMap(dataset => {
      if (dataset.subsets.isEmpty)
        dataset.datadumps.map(uri => (uri, dataset))
      else
        dataset.datadumps.map(uri => (uri, dataset)) ++ getDataDumpURLs(dataset.subsets)
    })
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
    Datasets.delete(subsetsRecursive)
    
    // Purge from index
    Global.index.dropDatasets(subsetsRecursive)
    Global.index.refresh()    
  }
  
  private def importData(dataset: Dataset, dumpfiles: Seq[TemporaryFile])(implicit s: Session) = {
    dumpfiles.foreach(dump => {
	  Logger.info("Importing data from " + dump.file.getName + " to " + dataset.title)
	  if (dump.file.getName.endsWith(CSV))
        CSVImporter.importRecogitoCSV(Source.fromFile(dump.file, UTF8), dataset)
      else
        PelagiosOAImporter.importPelagiosAnnotations(dump, dump.file.getName, dataset)
           
      dump.finalize()
      Logger.info(dump.file.getName + " - import complete.")
	})    
  }
      
  /** (Re-)Harvest a dataset from a VoID URL **/
  def harvest(voidURL: String, previous: Seq[Dataset] = Seq.empty[Dataset]) = {	  
    val startTime = System.currentTimeMillis
   
    // Assign a random (but unique) name, and keep the extension from the original file
    val voidFilename = { 
      val extension = voidURL.substring(voidURL.lastIndexOf("."))
      if (extension.indexOf("?") < 0)
        "void_" + UUID.randomUUID.toString + extension
      else
        "void_" + UUID.randomUUID.toString + extension.substring(0, extension.indexOf("?"))
    }
    val voidTempFile = downloadFile(voidURL, voidFilename)
    
    if (voidTempFile.isDefined) {	
	  val voidHash = computeHash(voidTempFile.get.file)
	
	  val datasets = VoIDImporter.readVoID(voidTempFile.get, voidFilename)
	  voidTempFile.get.finalize()
	  
	  val dataDumps = getDataDumpURLs(datasets).par.map { case (url, dataset) => {
	    val dumpFilename = "data_" + UUID.randomUUID.toString + url.substring(url.lastIndexOf("."))
	    (url, downloadFile(url, dumpFilename))
	  }}.seq
	  
	  if (dataDumps.filter(_._1.isEmpty).size == 0) {
	    
	    // TODO compare hashes and only ingest on change
	    
	    DB.withSession { implicit session: Session =>	    
	      previous.foreach(dataset => dropDatasetCascaded(dataset.id))
	      
	      val dumpfileMap = dataDumps.toMap.mapValues(_.get)	      
	      Logger.info("Importing dataset: " + datasets.map(_.title).mkString(", "))
	      val importedDatasetsWithDumpfiles = VoIDImporter.importVoID(datasets, Some(voidURL))
	      importedDatasetsWithDumpfiles.foreach { case (dataset, uris) => {
	        importData(dataset, uris.map(uri => dumpfileMap.get(uri).get))
	      }}
	    }
	  } else {
	    Logger.warn("Download failure - aborting " + voidURL)
	  }
    }
  }
	
}
