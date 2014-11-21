package controllers.admin

import global.Global
import java.io.File
import java.net.URL
import models.Associations
import models.core._
import play.api.db.slick._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{ AnyContent, Controller, SimpleResult }
import play.api.mvc.MultipartFormData.FilePart
import play.api.Logger
import play.api.libs.json.Json
import scala.io.Source
import sys.process._
import ingest._
import ingest.harvest.HarvestWorker
import ingest.harvest.HarvestWorker

object DatasetAdminController extends Controller with Secured {
  
  private val UTF8 = "UTF-8"
  private val CSV = "csv"
  private val TMP_DIR = System.getProperty("java.io.tmpdir")
  
  /** Generic boiler plate code needed for file upload **/
  private def processUpload(formFieldName: String, requestWithSession: DBSessionRequest[AnyContent], action: FilePart[TemporaryFile] => SimpleResult) = {
      val formData = requestWithSession.request.body.asMultipartFormData
      if (formData.isDefined) {
        try  {
          Logger.info("Processing upload...")
          val f = formData.get.file(formFieldName)
          if (f.isDefined)
            action(f.get) // This is where we execute the handler - the rest is sanity checking boilerplate
          else
            BadRequest(Json.parse("{\"message\": \"Invalid form data - missing file\"}"))
        } catch {
          case t: Throwable => {
            t.printStackTrace()
            Redirect(routes.DatasetAdminController.index).flashing("error" -> { "There is something wrong with the upload: " + t.getMessage })
          }
        }
      } else {
        BadRequest(Json.parse("{\"message\": \"Invalid form data\"}"))
      }      
  }
  
  def index = adminAction { username => implicit requestWithSession =>
    Ok(views.html.admin.datasets(Datasets.listAll()))
  }
  
  def uploadDataset = adminAction { username => implicit requestWithSession =>    
    val json = requestWithSession.request.body.asJson
    if (json.isDefined) {
      val url = (json.get \ "url").as[String]
      Logger.info("Downloading VoID from " + url)
      
      val filename = url.substring(url.lastIndexOf("/") + 1)
      val tempFile = new TemporaryFile(new File(TMP_DIR, filename))
	  new URL(url) #> tempFile.file !!
	  
	  Logger.info("Download complete - importing")
	  VoIDImporter.importVoID(tempFile, filename, Some(url))
	  Logger.info("Import complete")
      Ok(Json.parse("{ \"message\": \"New Dataset Created.\" }"))   
    } else {
      processUpload("void", requestWithSession, { filepart => {
        VoIDImporter.importVoID(filepart.ref, filepart.filename)
        Redirect(routes.DatasetAdminController.index).flashing("success" -> { "New Dataset Created." })      
      }})
    }
  }
  
  def reingestDataset(id: String) = adminAction { username => implicit requestWithSession =>
    // TODO dummy implementation!
    val dataset = Datasets.findById(id)
    if (dataset.isDefined) {
      val url = dataset.get.voidURI
      if (url.isDefined) {
        new HarvestWorker().fullHarvest(url.get, dataset)
      }
    }
  
    Ok("done")
  }
  
  def deleteDataset(id: String) = adminAction { username => implicit requestWithSession =>
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
    
    Status(200)
  }
  
  def uploadAnnotations(id: String) = adminAction { username => implicit requestWithSession => 
    processUpload("annotations", requestWithSession, { filepart => {
      val dataset = Datasets.findById(id)
      if (dataset.isDefined) {
        if (filepart.filename.endsWith(CSV))
          CSVImporter.importRecogitoCSV(Source.fromFile(filepart.ref.file, UTF8), dataset.get)
        else
          PelagiosOAImporter.importPelagiosAnnotations(filepart.ref, filepart.filename, dataset.get)
        Redirect(routes.DatasetAdminController.index).flashing("success" ->
          { "Annotations from file " + filepart.filename + " imported successfully." })
      } else {
        NotFound
      }
    }})
  }
  
  def harvestDataset(id: String) = adminAction { username => implicit requestWithSession =>
    val worker = new HarvestWorker()
    worker.harvest(id)
    Ok("")
  }
  
}
