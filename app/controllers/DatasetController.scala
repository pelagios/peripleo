package controllers

import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json

object DatasetController extends Controller {
  
  def listAll = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }
  
  def getDataset(id: String) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }
    
  def listAnnotatedThings(id: String) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }
  
}