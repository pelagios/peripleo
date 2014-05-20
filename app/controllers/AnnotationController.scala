package controllers

import play.api.mvc.Action
import play.api.libs.json.Json
import java.util.UUID

object AnnotationController extends AbstractAPIController {

  def listAll(prettyPrint: Option[Boolean]) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }
  
  def getAnnotation(id: UUID, prettyPrint: Option[Boolean]) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }
  
}