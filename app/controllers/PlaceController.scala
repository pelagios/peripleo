package controllers

import controllers.common.io.JSONWrites._
import global.Global
import index.Index
import index.places.IndexedPlace
import models.{ AggregatedView, Dataset }
import play.api.mvc.Action
import play.api.db.slick._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import models.Gazetteers

object PlaceController extends AbstractAPIController {

  def listAll(gazetteerName:String, limit: Option[Int], offset: Option[Int]) = DBAction { implicit session => 
    val gazetteer = Gazetteers.findByName(gazetteerName) 
    if (gazetteer.isDefined) {
      val allPlaces = Global.index.listAllPlaces(gazetteer.get.name, offset.getOrElse(0), limit.getOrElse(gazetteer.get.totalPlaces))
      jsonOk(Json.toJson(allPlaces), session.request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Place not found.\" }"))
    }
  }  

  def getPlace(uri: String) = Action { implicit request =>
    val place = Global.index.findPlaceByURI(uri)
    if (place.isDefined)
      jsonOk(Json.toJson(place.get), request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
  def getNetwork(uri: String) = Action { implicit request =>
    val network = Global.index.findNetworkByPlaceURI(uri)
    if(network.isDefined) {
      jsonOk(Json.toJson(network), request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
    }
  }
  
  def listOccurrences(uri: String, includeCloseMatches: Boolean) = DBAction { implicit request =>
    val place = Global.index.findPlaceByURI(Index.normalizeURI(uri))
    if (place.isDefined) {
      val occurrences = 
        if (includeCloseMatches) {
          val places = Global.index.findNetworkByPlaceURI(uri).get.places.map(_.uri)
          AggregatedView.findOccurrences(places.toSet) 
        } else {
          AggregatedView.findOccurrences(place.get.uri)      
        }
      implicit val verbose = false
      jsonOk(Json.toJson((place.get, occurrences)), request.request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
    }
  }
  
}