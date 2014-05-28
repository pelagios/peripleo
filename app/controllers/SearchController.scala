package controllers

import controllers.common.io.JSONWrites._
import global.Global
import play.api.db.slick._
import models.Page
import global.index.IndexedObject
import play.api.libs.json.Json

object SearchController extends AbstractAPIController {
  
  private val QUERY = "query"
  
  def search(query: String, limit: Int, offset: Int) = DBAction { implicit session =>
    val results = Global.index.search(query, offset, limit)
    jsonOk(Json.toJson(results), session.request)
  }

}