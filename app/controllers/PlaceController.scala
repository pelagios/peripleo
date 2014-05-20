package controllers

import play.api.mvc.Action
import play.api.libs.json.Json

object PlaceController extends AbstractAPIController {

  def listAll(prettyPrint: Option[Boolean]) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }  
  
  def getPlace(id: String, prettyPrint: Option[Boolean]) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }
  
}