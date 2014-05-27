package controllers

import controllers.common.io.JSONWrites._
import models._
import play.api.db.slick._
import play.api.libs.json.{ Json, JsValue }

object DatasetController extends AbstractAPIController {
  
  def listAll(offset: Int, limit: Int, prettyPrint: Boolean) = DBAction { implicit session =>
    jsonOk(Json.toJson(Datasets.listAll(offset, limit)), prettyPrint)
  }
  
  def getDataset(id: String, prettyPrint: Boolean) = DBAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined)
      jsonOk(Json.toJson(dataset.get), prettyPrint)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
    
  def listAnnotatedThings(id: String, offset: Int, limit: Int, prettyPrint: Boolean) = DBAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined)
      jsonOk(Json.toJson(AnnotatedThings.findByDataset(id, offset, limit)), prettyPrint)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
  def listPlaces(id: String, offset: Int, limit: Int, prettyPrint: Boolean) = DBAction { implicit session =>
    val places = Places.findPlacesInDataset(id, offset, limit)
    jsonOk(Json.toJson(places), prettyPrint)
  } 
  
}