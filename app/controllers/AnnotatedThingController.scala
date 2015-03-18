package controllers

import controllers.common.JSONWrites._
import models.Associations
import models.adjacency.PlaceAdjacencys
import models.core.{ Annotations, AnnotatedThings }
import play.api.db.slick._
import play.api.libs.json.{ Json, JsString, Writes }


object AnnotatedThingController extends AbstractController {
      
  def listAll(limit: Int, offset: Int) = loggingAction { implicit session =>
    jsonOk(Json.toJson(AnnotatedThings.listAll(false, offset, limit)), session.request)
  }  
  
  def listPlaceVectors(limit: Int, offset: Int) = DBAction { implicit session =>
    val things = AnnotatedThings.listAll(true, offset, limit)
    val vectors = Associations.findPlaceVectorsForThings(things.items.map(_.id))

    val response = things.items.map(thing => {
      thing.id + ";" +
      thing.title + ";" +
      thing.dataset + ";" +
      Annotations.countByAnnotatedThing(thing.id, true) + ";" +
      vectors.get(thing.id).map(_.mkString(",")).getOrElse("")
    }).mkString("\n")
    
    Ok(response)
  }
  
  def getAnnotatedThing(id: String) = DBAction { implicit session =>
    val thing = AnnotatedThings.findById(id)
    if (thing.isDefined)
      jsonOk(Json.toJson(thing.get), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }  
  
  def listSubThings(id: String, limit: Int, offset: Int) = loggingAction { implicit session =>
    val subItems = AnnotatedThings.listChildren(id)
    jsonOk(Json.toJson(subItems), session.request)
  }
  
  def listPlaces(id: String, limit: Int, offset: Int) = loggingAction { implicit session =>
    val places = Associations.findPlacesForThing(id)
    jsonOk(Json.toJson(places), session.request)
  } 
  
  def listAnnotations(id: String, limit: Int, offset: Int) = loggingAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined)
      jsonOk(Json.toJson(Annotations.findByAnnotatedThing(id)), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  } 
  
  def getAdjacencyGraph(id: String) = loggingAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined) {
      jsonOk(Json.toJson(PlaceAdjacencys.findByAnnotatedThingRecursive(id)), session.request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
    }
  }
  
}
