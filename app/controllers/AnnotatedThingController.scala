package controllers

import java.util.UUID
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.libs.json.{ Json, JsString, Writes }
import models._
import global.Global

object AnnotatedThingController extends Controller {
  
  // Implicit JSON serializers
  implicit private val serializeAnnotatedThing = Json.writes[AnnotatedThing]
  implicit private val serializeUUID = Writes { uuid: UUID => JsString(uuid.toString) } // UUIDs are not supported out of the box
  implicit private val serializeAnnotation = Json.writes[Annotation]
  
  import controllers.common.io.JSONWriter._
  
  implicit private val serializePlacesPerThing = new Writes[Page[(String, Int)]] {
    def writes(page: Page[(String, Int)]) = Json.obj(
      "total" -> page.total,
      "offset" -> page.offset,
      "limit" -> page.limit,
      "items" -> page.items.map { case (gazetteerURI, count) => Json.toJson(Global.index.findByURI(gazetteerURI)) }
    )
  }
  
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
