package controllers.admin

import controllers.common.io.{ CSVImporter, VoIDImporter }
import models.Datasets
import play.api.db.slick._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{ AnyContent, Controller, SimpleResult }
import play.api.mvc.MultipartFormData.FilePart
import scala.io.Source
import play.api.Logger
import play.api.libs.json.Json

object DatasetAdminController extends Controller with Secured {
  
  private val UTF8 = "UTF-8"
  
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
    processUpload("void", requestWithSession, { filepart => {
      VoIDImporter.importVoID(filepart)
      Redirect(routes.DatasetAdminController.index).flashing("success" -> { "New Dataset Created." })      
    }})
  }
  
  def deleteDataset(id: String) = adminAction { username => implicit requestWithSession =>
    Datasets.delete(id)
    Status(200)
  }
  
  def uploadAnnotations(id: String) = adminAction { username => implicit requestWithSession => 
    processUpload("annotations", requestWithSession, { filepart => {
      val dataset = Datasets.findById(id)
      if (dataset.isDefined) {
        CSVImporter.importRecogitoCSV(Source.fromFile(filepart.ref.file, UTF8), dataset.get)
        Redirect(routes.DatasetAdminController.index).flashing("success" -> { "Annotations imported." })
      } else {
        NotFound
      }
    }})
  }
  
}