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

}