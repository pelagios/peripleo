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
import ingest.harvest.Harvester

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
      Logger.info("Importing dataset from " + url)
      
      Harvester.harvest(url)
      
      Ok(Json.parse("{ \"message\": \"New Dataset Created.\" }"))   
    } else {
      processUpload("void", requestWithSession, { filepart => {
        VoIDImporter.importVoID(filepart.ref)
        Redirect(routes.DatasetAdminController.index).flashing("success" -> { "New Dataset Created." })      
      }})
    }
  }
  
  def harvestDataset(id: String) = adminAction { username => implicit requestWithSession =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined) {
      val uri = dataset.get.voidURI
      if (uri.isDefined) {
        Harvester.harvest(uri.get, Datasets.findTopLevelByVoID(uri.get))
      }
    }
  
    Ok(Json.parse("{ \"message\": \"Harvest Running\" }"))
  }
  
  def deleteDataset(id: String) = adminAction { username => implicit requestWithSession =>
    val dataset = Datasets.findById(id)
    
    if (dataset.isDefined) {
      Logger.info("Deleting dataset " + dataset.get.title)
      
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
        else
          PelagiosOAImporter.importPelagiosAnnotations(filepart.ref, filepart.filename, dataset.get)
        Redirect(routes.DatasetAdminController.index).flashing("success" ->
          { "Annotations from file " + filepart.filename + " imported successfully." })
      } else {
        NotFound
      }
    }})
  }
  
}
