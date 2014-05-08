package controllers

import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import java.util.UUID

object AnnotationController extends Controller {

  def listAll = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }
  
  def getAnnotation(id: UUID) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }
  
}