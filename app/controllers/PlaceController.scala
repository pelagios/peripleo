package controllers

import controllers.common.JSONWrites._
import global.Global
import index.Index
import index.places.IndexedPlace
import models.Associations
import models.core.Dataset
import play.api.mvc.Action
import play.api.db.slick._
import play.api.libs.json.Json
import models.geo.Gazetteers

object PlaceController extends AbstractController {

  def listAll(gazetteerName:String, bbox: Option[String], limit: Option[Int], offset: Option[Int]) = loggingAction { implicit session => 
    // Map BBox coordinates
    val bboxTupled = bbox.flatMap(str => {
      val coords = str.split(",").map(_.trim)
      try {
        Some((coords(0).toDouble, coords(1).toDouble, coords(2).toDouble, coords(3).toDouble))
      } catch {
        case _: Throwable => None
      }
    })
    
    val gazetteer = Gazetteers.findByName(gazetteerName) 
    if (gazetteer.isDefined) {
      val allPlaces = Global.index.listAllPlaces(gazetteer.get.name.toLowerCase, bboxTupled, offset.getOrElse(0), limit.getOrElse(20))
      jsonOk(Json.toJson(allPlaces), session.request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Place not found.\" }"))
    }
  } 
  
  def listItemVectors(limit: Int, offset: Int) = DBAction { implicit session =>
    // val places = Global.index.listAllPlaceNetworks(offset, limit).flatMap(_.places).map(p => (p.label, p.uri))
    val vectors = Associations.findThingVectorsForPlaces()
    val response = vectors.keySet.map(uri => {
      val place = Global.index.findPlaceByURI(uri)
    
      uri + ";" +
        place.map(_.label).getOrElse("?") + ";" +
        vectors.get(uri).map(_.mkString(",")).getOrElse("")
    }).mkString("\n")
    
    Ok(response)
  }

  def getPlace(uri: String) = loggingAction { implicit session =>
    val place = Global.index.findPlaceByURI(uri)
    if (place.isDefined)
      jsonOk(Json.toJson(place.get), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
  def getNetwork(uri: String) = loggingAction { implicit session =>
    val network = Global.index.findNetworkByPlaceURI(uri)
    if(network.isDefined) {
      jsonOk(Json.toJson(network), session.request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
    }
  }
  
  def listOccurrences(uri: String, includeCloseMatches: Boolean) = loggingAction { implicit request =>
    val place = Global.index.findPlaceByURI(Index.normalizeURI(uri))
    if (place.isDefined) {
      val occurrences = 
        if (includeCloseMatches) {
          val places = Global.index.findNetworkByPlaceURI(uri).get.places.map(_.uri)
          Associations.findOccurrences(places.toSet) 
        } else {
          Associations.findOccurrences(place.get.uri)      
        }
      implicit val verbose = false
      jsonOk(Json.toJson((place.get, occurrences)), request.request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
    }
  }
  
}