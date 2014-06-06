package controllers

import models._
import controllers.common.io.JSONWrites._
import play.api.db.slick._
import play.api.libs.json.{ Json, JsString, Writes }

object AnnotatedThingController extends AbstractAPIController {
    
  def listAll(limit: Int, offset: Int) = DBAction { implicit session =>
    jsonOk(Json.toJson(AnnotatedThings.listAll(false, offset, limit)), session.request)
  }  
  
  def getAnnotatedThing(id: String) = DBAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined)
      jsonOk(Json.toJson(annotatedThing.get), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }  
  
  def listSubItems(id: String, limit: Int, offset: Int) = DBAction { implicit session =>
    val subItems = AnnotatedThings.listChildren(id)
    jsonOk(Json.toJson(subItems), session.request)
  }
  
  def listPlaces(id: String, limit: Int, offset: Int) = DBAction { implicit session =>
    val places = Places.findPlacesForThing(id)
    jsonOk(Json.toJson(places), session.request)
  } 
  
  def listAnnotations(id: String, limit: Int, offset: Int) = DBAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined)
      jsonOk(Json.toJson(Annotations.findByAnnotatedThing(id)), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  } 
  
}
