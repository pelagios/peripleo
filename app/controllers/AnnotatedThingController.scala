package controllers

import models._
import controllers.common.io.JSONWrites._
import play.api.db.slick._
import play.api.libs.json.{ Json, JsString, Writes }

object AnnotatedThingController extends AbstractAPIController {
    
  def listAll(offset: Int, limit: Int) = DBAction { implicit session =>
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }  
  
  def getAnnotatedThing(id: String) = DBAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined)
      jsonOk(Json.toJson(annotatedThing.get), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }  
  
  def listPlaces(id: String, offset: Int, limit: Int) = DBAction { implicit session =>
    val places = Places.findPlacesForThing(id)
    jsonOk(Json.toJson(places), session.request)
  } 
  
  def listAnnotations(id: String, offset: Int, limit: Int) = DBAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined)
      jsonOk(Json.toJson(Annotations.findByAnnotatedThing(id)), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  } 
  
}
