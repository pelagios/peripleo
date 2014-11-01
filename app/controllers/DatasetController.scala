package controllers

import controllers.common.io.JSONWrites._
import models.Associations
import models.core.{ AnnotatedThings, Datasets }
import play.api.db.slick._
import play.api.libs.json.{ Json, JsValue }

object DatasetController extends AbstractAPIController {
  
  def listAll(limit: Int, offset: Int) = loggingAction { implicit session =>
    jsonOk(Json.toJson(Datasets.listAll(true, offset, limit)), session.request)
  }
  
  def getDataset(id: String) = loggingAction { implicit session =>
    val dataset = Datasets.findByIdWithDumpfiles(id)
    if (dataset.isDefined)
      jsonOk(Json.toJson(dataset.get), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
  def getTemporalProfile(id: String) = loggingAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined)
      jsonOk(Json.parse(dataset.get.temporalProfile.getOrElse("{}")), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
    
  def listAnnotatedThings(id: String, limit: Int, offset: Int) = loggingAction { implicit session =>
    val dataset = Datasets.findByIdWithDumpfiles(id)
    if (dataset.isDefined)
      jsonOk(Json.toJson(AnnotatedThings.findByDataset(id, true, true, offset, limit)), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
  def listPlaces(id: String, limit: Int, offset: Int) = loggingAction { implicit session =>
    val places = Associations.findPlacesInDataset(id, offset, limit)
    
    implicit val verbose = session.request.queryString
      .filter(_._1.toLowerCase.equals("verbose"))
      .headOption.flatMap(_._2.headOption.map(_.toBoolean)).getOrElse(true)
      
    jsonOk(Json.toJson(places), session.request)
  }
  
}
