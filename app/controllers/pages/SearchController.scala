package controllers.pages

import controllers.AbstractController
import global.Global
import index.Heatmap
import index.objects.IndexedObject
import models.Page
import models.core.Datasets
import play.api.db.slick._
import play.api.libs.json.Json
import play.api.mvc.Action
import scala.util.{ Success, Failure }

object SearchController extends AbstractController {
  
  def search() = loggingAction { implicit session =>
    parseSearchParams(session.request) match {
      case Success(params) => {
        val startTime = System.currentTimeMillis
        
        // Format filter screen names
        // val filters = Seq(
          // params.objectType.map(typ => ("type", typ.toString)),
          // params.dataset.flatMap(datasetId => Datasets.findById(datasetId).map(dataset => ("dataset", dataset.title)))
        // ).flatten.toMap

        // Search
        // val results = 
        //  Global.index.search(params)
            
        // Ok(views.html.newSearch(results._1, Some(results._2), filters, results._4, System.currentTimeMillis - startTime))
        Ok("Not implemented")
      }
            
      case Failure(exception) => // TODO error page
        Ok(views.html.newSearch(Page.empty[(IndexedObject, Option[String])], None, Map.empty[String, String], Heatmap.empty, 0))
    }
  }
  
  def autoSuggest(query: String) = Action {
    // We try exact matches first, and fuzzy matches from the suggester if no exact matches
    val suggestions = { 
      val exactMatches = Global.index.suggester.suggestCompletion(query, 5)
      if (exactMatches.size > 0)
        exactMatches
      else
        Global.index.suggester.suggestSimilar(query, 5)
    }

    Ok(Json.toJson(suggestions.map(result => Json.obj("key" -> result)) ))
  }

}