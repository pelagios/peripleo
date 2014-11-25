package controllers.admin

import play.api.Logger
import play.api.mvc.{ AnyContent, Controller, SimpleResult }
import play.api.mvc.MultipartFormData.FilePart
import play.api.db.slick.DBSessionRequest
import play.api.libs.json.Json
import play.api.libs.Files.TemporaryFile

class BaseUploadController extends Controller {
  
  /** Generic boiler plate code needed for file upload **/
  protected def processUpload(formFieldName: String, requestWithSession: DBSessionRequest[AnyContent], action: FilePart[TemporaryFile] => SimpleResult) = {
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

}