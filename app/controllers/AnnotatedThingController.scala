package controllers

import controllers.common.io.JSONWriter._
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.libs.json.{ Json, JsString, Writes }
import models._

object AnnotatedThingController extends Controller {
    
  def listAll = DBAction { implicit session =>
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }  
  
  def getAnnotatedThing(id: String) = DBAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined)
      Ok(Json.prettyPrint(Json.toJson(annotatedThing)))
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }  
  
  def listPlaces(id: String) = DBAction { implicit session =>
    val places = Places.findPlacesForThing(id)
    Ok(Json.prettyPrint(Json.toJson(places)))
  } 
  
  def listAnnotations(id: String) = DBAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined)
      Ok(Json.prettyPrint(Json.toJson(Annotations.findByAnnotatedThing(id).items)))
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  } 
  
}
