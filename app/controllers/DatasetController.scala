package controllers

import controllers.common.io.JSONWriter._
import models._
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.libs.json.Json

object DatasetController extends Controller {
  
  def listAll = DBAction { implicit session =>
    Ok(Json.prettyPrint(Json.toJson(Datasets.listAll().items)))
  }
  
  def getDataset(id: String) = DBAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined)
      Ok(Json.prettyPrint(Json.toJson(dataset)))
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
    
  def listAnnotatedThings(id: String) = DBAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined)
      Ok(Json.prettyPrint(Json.toJson(AnnotatedThings.findByDataset(id).items)))
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
  def listPlaces(id: String) = DBAction { implicit session =>
    val places = Places.findPlacesInDataset(id).items
    Ok(Json.prettyPrint(Json.toJson(places)))
  } 
  
}