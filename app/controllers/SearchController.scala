package controllers

import controllers.common.JSONWrites._
import global.Global
import play.api.db.slick._
import play.api.libs.json.Json
import scala.util.{ Success, Failure }
import play.api.libs.json.JsObject

object SearchController extends AbstractController {

  private val KEY_FACETS = "facets"
  private val KEY_TIME_HISTOGRAM = "timehistogram"
  private val KEY_HEATMAP = "heatmap"
    
  /** API search method controller.
    * 
    * @param limit search result page size
    * @param offset search result page offset
    * @param query keyword query
    * @param objectType filter search to a specific object type ('place', 'item' or 'dataset')
    * @param dataset filter search to items in a specific dataset
    * @param places filter search to items referencing specific places 
    * @param yearFrom start year for temporal constraint
    * @param yearTo end year for temporal constraint
    */
  def search() = loggingAction { implicit session =>      
    parseSearchParams(session.request) match {
      case Success(params) => {    
        // Facets, time histogram and heatmaps are optional
        val includeFacets = getQueryParam(KEY_FACETS, session.request).map(_.toBoolean).getOrElse(false)
        val includeTimeHistogram = getQueryParam(KEY_TIME_HISTOGRAM, session.request).map(_.toBoolean).getOrElse(false)
        val includeHeatmap = getQueryParam(KEY_HEATMAP, session.request).map(_.toBoolean).getOrElse(false)
        
        val (results, facetTree, timeHistogram, heatmap) = 
          Global.index.search(params,
            includeFacets, 
            true, // We always want preview snippets
            includeTimeHistogram,
            includeHeatmap)
        
        val jsonComponents = Seq(
              { facetTree.map(Json.toJson(_).as[JsObject]) },
              { timeHistogram.map(Json.toJson(_).as[JsObject]) },
              { heatmap.map(h => Json.obj("heatmap" -> Json.toJson(h)).as[JsObject]) }).flatten
         
        implicit val verbose = getQueryParam("verbose", session.request).map(_.toBoolean).getOrElse(false)          
        val response =
          jsonComponents.foldLeft(Json.toJson(results.map(_._1)).as[JsObject])((response, payload) => response ++ payload)
        
        jsonOk(response, session.request)
      }
      
      case Failure(exception) => BadRequest(Json.parse("{ \"error\": \"" + exception.getMessage + "\" }"))
    }
  }

}
