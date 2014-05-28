package controllers

import controllers.common.io.JSONWrites._
import global.Global
import play.api.mvc.Action
import play.api.libs.json.Json

object PlaceController extends AbstractAPIController {

  def listAll(limit: Int, offset: Int) = Action {
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }  
  
  def getPlace(uri: String) = Action { implicit request =>
    val place = Global.gazetteer.findByURI(uri)
    if (place.isDefined)
      jsonOk(Json.toJson(place.get), request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
}