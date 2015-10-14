package controllers

import controllers.common.JSONWrites._
import global.Global
import play.api.db.slick._
import play.api.mvc.Action
import play.api.libs.json.{ Json, JsObject }
import scala.util.{ Success, Failure }
import java.util.Locale

object SearchController extends AbstractController {

  private val KEY_TOP_PLACES = "top_places"
  private val KEY_FACETS = "facets"
  private val KEY_TIME_HISTOGRAM = "time_histogram"
  private val KEY_HEATMAP = "heatmap"
  private val KEY_HAS_IMAGES = "has_images"
  
  private val LOCALE_EN = new Locale("en")
        
  private val facetFormatters = Map(
    "lang" ->
      { value: String => new Locale(value).getDisplayLanguage(LOCALE_EN) },
    "source_dataset" -> 
      { value: String =>
        if (value.startsWith("gazetteer:"))
          value.substring(10)
        else if (value.contains("#"))
          value.substring(0, value.indexOf('#'));
        else
          value
      }
  )
    
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
        
        // Only include results with images?
        val onlyWithImages = getQueryParam(KEY_HAS_IMAGES, session.request).map(_.toBoolean).getOrElse(false)
        
        val (results, facetTree, timeHistogram, topPlaces, heatmap) = 
          Global.index.search(params,
            includeFacets, 
            true, // We always want preview snippets
            includeTimeHistogram,
            includeTopPlaces,
            includeHeatmap,
            onlyWithImages)
       
        // Compile the JSON response from the various optional components
        implicit val verbose = getQueryParam("verbose", session.request).map(_.toBoolean).getOrElse(false)   
        
        val optionalComponents = Seq(
              { facetTree.map(tree => Json.toJson((tree, facetFormatters)).as[JsObject]) },
              { timeHistogram.map(Json.toJson(_).as[JsObject]) },
              { topPlaces.map(t => Json.obj("top_places" -> Json.toJson(t)).as[JsObject]) },
              { heatmap.map(h => Json.obj("heatmap" -> Json.toJson(h)).as[JsObject]) }).flatten
               
        val response = 
          optionalComponents.foldLeft(Json.toJson(results).as[JsObject])((response, payload) => response ++ payload) ++
          Json.obj("took_ms" -> (System.currentTimeMillis - startTime))
        
        jsonOk(response, session.request)
      }
      
      case Failure(exception) => BadRequest(Json.parse("{ \"error\": \"" + exception.getMessage + "\" }"))
    }
  }
  
  def autoComplete(query: String) = Action {
    // We try exact matches first, and fuzzy matches from the suggester if no exact matches
    val suggestions = { 
      val exactMatches = Global.index.suggester.suggestCompletion(query, 5)
      if (exactMatches.size > 0)
        exactMatches
      else
        Global.index.suggester.suggestSimilar(query, 5)
    }

    Ok(Json.toJson(suggestions.map(result => Json.obj("val" -> result)) ))
  }

}
