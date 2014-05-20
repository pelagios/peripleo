package controllers

import controllers.common.io.JSONWrites._
import models._
import play.api.db.slick._
import play.api.libs.json.{ Json, JsValue }

object DatasetController extends AbstractAPIController {
  
  def listAll(prettyPrint: Option[Boolean]) = DBAction { implicit session =>
    jsonOk(Json.toJson(Datasets.listAll()), prettyPrint)
  }
  
  def getDataset(id: String, prettyPrint: Option[Boolean]) = DBAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined)
      jsonOk(Json.toJson(dataset), prettyPrint)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
    
  def listAnnotatedThings(id: String, prettyPrint: Option[Boolean]) = DBAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined)
      jsonOk(Json.toJson(AnnotatedThings.findByDataset(id)), prettyPrint)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
  def listPlaces(id: String, prettyPrint: Option[Boolean]) = DBAction { implicit session =>
    val places = Places.findPlacesInDataset(id)
    jsonOk(Json.toJson(places), prettyPrint)
  } 
  
}