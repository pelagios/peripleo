package controllers

import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json

object PlaceController extends Controller {

  def listAll = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }  
  
  def getPlace(id: String) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }
  
}