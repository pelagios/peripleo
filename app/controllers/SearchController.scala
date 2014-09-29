package controllers

import controllers.common.io.JSONWrites._
import global.Global
import models.{ Page, AnnotatedThing, Places }
import play.api.db.slick._
import play.api.libs.json.Json

object SearchController extends AbstractAPIController {
  
  def search(query: String, limit: Int, offset: Int) = DBAction { implicit session =>
    val results = Global.index.search(query, offset, limit)
    jsonOk(Json.toJson(results), session.request)
  }
  
  def filter(limit: Int, offset: Int, dataset: Option[String], places: Option[String]) = DBAction { implicit session =>
    // TODO just a hack - we're hardcoding the object type to ANNOTATED_THING
    if (dataset.isDefined && places.isDefined) {
      val result = Places.findThingsForPlaceAndDataset(places.get, dataset.get)
      val mapped = Page(result.map(_._1), offset, limit, result.size)
      jsonOk(Json.toJson(mapped), session.request)
    } else if (dataset.isDefined) {
      Ok("")
    } else {
      Ok("")
    }
  }

}