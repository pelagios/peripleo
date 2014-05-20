package controllers

import models._
import controllers.common.io.JSONWrites._
import play.api.db.slick._
import play.api.libs.json.{ Json, JsString, Writes }

object AnnotatedThingController extends AbstractAPIController {
    
  def listAll(prettyPrint: Option[Boolean]) = DBAction { implicit session =>
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }  
  
  def getAnnotatedThing(id: String, prettyPrint: Option[Boolean]) = DBAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined)
      jsonOk(Json.toJson(annotatedThing.get), prettyPrint)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }  
  
  def listPlaces(id: String, prettyPrint: Option[Boolean]) = DBAction { implicit session =>
    val places = Places.findPlacesForThing(id)
    jsonOk(Json.toJson(places), prettyPrint)
  } 
  
  def listAnnotations(id: String, prettyPrint: Option[Boolean]) = DBAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined)
      jsonOk(Json.toJson(Annotations.findByAnnotatedThing(id)), prettyPrint)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  } 
  
}
