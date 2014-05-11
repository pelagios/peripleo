package controllers.admin

import controllers.common.io.VoIDImporter
import models.Datasets
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.libs.json.Json

object DatasetAdminController extends Controller with Secured {

  def index = adminAction { username => implicit session =>
    Ok(views.html.admin.datasets(Datasets.listAll()))
  }
  
  def uploadDataset = adminAction { username => implicit session =>
    val formData = session.request.body.asMultipartFormData
    if (formData.isDefined) {
      try {
        val f = formData.get.file("void")
        if (f.isDefined) {
          VoIDImporter.importVoID(f.get)(session.dbSession, session.request)
          Redirect(routes.DatasetAdminController.index).flashing("success" -> { "New Dataset Created." })
        } else {
          BadRequest
        }
      } catch {
        case t: Throwable => Redirect(routes.DatasetAdminController.index).flashing("error" -> { "There is something wrong with the upload: " + t.getMessage })
      }
    } else {
      BadRequest
    }
  }
  
  def deleteDataset(id: String) = adminAction { username => implicit session =>
    Datasets.delete(id)
    Status(200)
  }
  
  def uploadAnnotations(id: String) = adminAction { username => implicit session => 
    Ok(Json.parse("{ \"message\":\"Upload to " + id + "\"}"))
  }
  
}