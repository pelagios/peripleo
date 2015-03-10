package controllers.pages

import controllers.AbstractController
import play.api.Logger
import play.api.mvc.Action
import global.Global
import play.api.libs.json.Json
import models.Page
import index.objects.IndexedObject
import play.api.db.slick._

object SearchController extends AbstractController {
  
  def search(query: Option[String]) = loggingAction { implicit session =>
    if (query.isDefined) {
      val startTime = System.currentTimeMillis
      val results = Global.index.search(20, 0, query)
      Ok(views.html.newSearch(results, System.currentTimeMillis - startTime))
    } else {
      // TODO redirect to home
      Ok(views.html.newSearch(Page.empty[IndexedObject], 0))
    }
  }
  
  def autoSuggest(query: String) = Action {
    val suggestions = Global.index.suggester.suggestSimilar(query, 5)
    val asJson = suggestions.map(result => Json.obj(
      "key" -> result
    ))
    
    Ok(Json.toJson(asJson))
  }

}