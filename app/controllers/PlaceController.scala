package controllers

import controllers.common.io.JSONWrites._
import global.Global
import play.api.mvc.Action
import play.api.libs.json.Json

object PlaceController extends AbstractAPIController {

  def listAll(limit: Int, offset: Int) = Action { implicit request => 
    jsonOk(Json.toJson(Global.index.listAllPlaces(offset, limit)), request)
  }  
  
  def getPlace(uri: String) = Action { implicit request =>
    val place = Global.index.findPlaceByURI(uri)
    if (place.isDefined)
      jsonOk(Json.toJson(place.get), request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
  def getNetwork(uri: String) = Action { implicit request =>
    val place = Global.index.findPlaceByURI(uri)
    if(place.isDefined) {
      val network = Global.index.getNetwork(place.get)
      jsonOk(Json.toJson(network), request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
    }
  }
  
}