package controllers.admin

import controllers.common.io.CSVImporter
import play.api.mvc.Controller
import scala.io.Source

object DatasetAdminController extends Controller with Secured {

  def index = adminAction { username => implicit session =>
    Ok(views.html.admin.datasets())
  }
  
  def uploadDataset = adminAction { username => implicit session =>
    val formData = session.request.body.asMultipartFormData
    if (formData.isDefined) {
      try {
        val f = formData.get.file("csv")
        if (f.isDefined) {
          val importer = new CSVImporter()
          importer.importRecogitoCSV(Source.fromFile(f.get.ref.file))(session.dbSession)
          Redirect(routes.DatasetAdminController.index).flashing("success" -> { "Uploaded CSV." })
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
  
}