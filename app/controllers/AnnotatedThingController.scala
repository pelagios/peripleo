package controllers

import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json

object AnnotatedThingController extends Controller {

  def listAll = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }  
  
  def getAnnotatedThing(id: String) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }  
  
  def listPlaces(id: String) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  } 
  
  def listAnnotations(id: String) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  } 
  
}
