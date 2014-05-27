package controllers

import controllers.common.io.JSONWrites._
import global.Global
import play.api.mvc.Action
import play.api.libs.json.Json

object PlaceController extends AbstractAPIController {

  def listAll(prettyPrint: Boolean) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }  
  
  def getPlace(uri: String, prettyPrint: Boolean) = Action {
    val place = Global.gazetteer.findByURI(uri)
    if (place.isDefined)
      jsonOk(Json.toJson(place.get), prettyPrint)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
}