package controllers

import controllers.common.JSONWrites._
import global.Global
import play.api.db.slick._
import play.api.libs.json.Json
import scala.util.{ Success, Failure }
import play.api.libs.json.JsObject
import index.IndexFields
import play.api.Logger

object SearchController extends AbstractController {

  private val KEY_TOP_PLACES = "top_places"
  private val KEY_FACETS = "facets"
  private val KEY_TIME_HISTOGRAM = "time_histogram"
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
    val startTime = System.currentTimeMillis
    
    parseSearchParams(session.request) match {
      case Success(params) => {    

        // Number of top places to include in JSON response, if any
        val includeTopPlaces = getQueryParam(KEY_TOP_PLACES, session.request).map(_.toInt).getOrElse(0)
        
        // Include facets in JSON response? 
        val includeFacets = getQueryParam(KEY_FACETS, session.request).map(_.toBoolean).getOrElse(false)
        
        // Include time histogram or heatmap?        
        val includeTimeHistogram = getQueryParam(KEY_TIME_HISTOGRAM, session.request).map(_.toBoolean).getOrElse(false)
        val includeHeatmap = getQueryParam(KEY_HEATMAP, session.request).map(_.toBoolean).getOrElse(false)
        
        val (results, facetTree, timeHistogram, topPlaces, heatmap) = 
          Global.index.search(params,
            includeFacets, 
            true, // We always want preview snippets
            includeTimeHistogram,
            includeTopPlaces,
            includeHeatmap)
       
        // Compile the JSON response from the various optional components
        implicit val verbose = getQueryParam("verbose", session.request).map(_.toBoolean).getOrElse(false)   
        
        val optionalComponents = Seq(
              { facetTree.map(Json.toJson(_).as[JsObject]) },
              { timeHistogram.map(Json.toJson(_).as[JsObject]) },
              { topPlaces.map(t => Json.obj("top_places" -> Json.toJson(t.map(_._1))).as[JsObject]) },
              { heatmap.map(h => Json.obj("heatmap" -> Json.toJson(h)).as[JsObject]) }).flatten
               
        val response = 
          optionalComponents.foldLeft(Json.toJson(results).as[JsObject])((response, payload) => response ++ payload) ++
          Json.obj("took_ms" -> (System.currentTimeMillis - startTime))
        
        jsonOk(response, session.request)
      }
      
      case Failure(exception) => BadRequest(Json.parse("{ \"error\": \"" + exception.getMessage + "\" }"))
    }
  }

}
