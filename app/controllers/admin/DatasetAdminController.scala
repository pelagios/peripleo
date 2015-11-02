package controllers.admin

import collection.JavaConverters._
import global.Global
import ingest._
import ingest.harvest.DataHarvester
import java.io.File
import java.util.zip.{ ZipEntry, ZipFile }
import models.Associations
import models.core._
import play.api.db.slick._
import play.api.Logger
import play.api.libs.json.Json
import scala.io.Source

object DatasetAdminController extends BaseUploadController with Secured {
  
  private val UTF8 = "UTF-8"
  private val CSV = "csv"
  private val TEIXML = "tei.xml"
  private val ZIP = "zip"
  
  private val TMP_DIR = System.getProperty("java.io.tmpdir")
  
  def index = adminAction { username => implicit requestWithSession =>
    Ok(views.html.admin.datasets(Datasets.listAll()))
  }
  
  def uploadDataset = adminAction { username => implicit requestWithSession =>    
    val json = requestWithSession.request.body.asJson
    if (json.isDefined) {
      val url = (json.get \ "url").as[String]
      Logger.info("Importing dataset from " + url)
      
      DataHarvester.harvest(url)
      
      Ok(Json.parse("{ \"message\": \"New Dataset Created.\" }"))   
    } else {
      processUpload("void", requestWithSession, { filepart => {
        VoIDImporter.importVoID(filepart.ref, filepart.filename)
        Redirect(routes.DatasetAdminController.index).flashing("success" -> { "New Dataset Created." })      
      }})
    }
  }
  
  def harvestDataset(id: String) = adminAction { username => implicit requestWithSession =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined) {
      val uri = dataset.get.voidURI
      if (uri.isDefined) {
        DataHarvester.harvest(uri.get, Datasets.findTopLevelByVoID(uri.get))
      }
    }
  
    Ok(Json.parse("{ \"message\": \"Harvest Running\" }"))
  }
  
  def deleteDataset(id: String) = adminAction { username => implicit requestWithSession =>
    val dataset = Datasets.findById(id)
    
    if (dataset.isDefined) {
      Logger.info("Deleting dataset " + dataset.get.title)
      
      val subsetsRecursive = id +: Datasets.listSubsetsRecursive(id)
    
      // Purge from databaseimport java.io.File

      Logger.info("Dropping annotations")
      Annotations.deleteForDatasets(subsetsRecursive)
      
      Logger.info("Dropping associations")
      Associations.deleteForDatasets(subsetsRecursive)
      
      Logger.info("Dropping images")
      Images.deleteForDatasets(subsetsRecursive)
      
      Logger.info("Dropping annotated things")
      AnnotatedThings.deleteForDatasets(subsetsRecursive)
      
      Logger.info("Dropping datasets")
      Datasets.delete(subsetsRecursive)
    
      // Purge from index
      Logger.info("Updating index")
      Global.index.dropDataset(id)
      Global.index.refresh()
      
      Logger.info("Done.")
      Status(200)
    } else {
      NotFound
    }
  }
  
  def uploadAnnotations(id: String) = adminAction { username => implicit requestWithSession => 
    processUpload("annotations", requestWithSession, { filepart => {
      val dataset = Datasets.findById(id)
      if (dataset.isDefined) {
        if (filepart.filename.endsWith(CSV))
          CSVImporter.importRecogitoCSV(Source.fromFile(filepart.ref.file, UTF8), dataset.get)
        else if (filepart.filename.endsWith(TEIXML)) 
          TEIImporter.importTEI(Source.fromFile(filepart.ref.file, UTF8), dataset.get)
        else if (filepart.filename.endsWith(ZIP)) 
          importZip(filepart.ref.file, dataset.get)
        else
          PelagiosOAImporter.importPelagiosAnnotations(filepart.ref, filepart.filename, dataset.get)
        Redirect(routes.DatasetAdminController.index).flashing("success" ->
          { "Annotations from file " + filepart.filename + " imported successfully." })
      } else {
        NotFound
      }
    }})
  }
  
  def rebuildAutoSuggestionIndex = adminAction { username => implicit requestWithSession => 
    Global.index.suggester.build()
    Status(200)
  }
  
  private def importZip(file: File, dataset: Dataset)(implicit s: Session) = {
    val zipFile = new ZipFile(file)
    val entries = zipFile.entries.asScala.toSeq.filter(!_.getName.startsWith("__MACOSX"))
    entries.foreach(entry => {
      Logger.info("Importing " + entry.getName)
      if (entry.getName.endsWith(TEIXML))
        TEIImporter.importTEI(Source.fromInputStream(zipFile.getInputStream(entry), UTF8), dataset)
      else if (entry.getName.endsWith(CSV))
        CSVImporter.importRecogitoCSV(Source.fromInputStream(zipFile.getInputStream(entry), UTF8), dataset)
      else // Temporary hack! Everything un-identified is treated as TEI
        TEIImporter.importTEI(Source.fromInputStream(zipFile.getInputStream(entry), UTF8), dataset)
    })
  }
  
}
