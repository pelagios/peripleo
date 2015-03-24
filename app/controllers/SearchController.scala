package controllers

import controllers.common.JSONWrites._
import global.Global
import play.api.db.slick._
import play.api.libs.json.Json
import scala.util.{ Success, Failure }
import play.api.libs.json.JsObject

object SearchController extends AbstractController {

  private val KEY_FACETS = "facets"
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
        val results = 
          Global.index.search(params.query, params.objectType, params.dataset, params.gazetteer, params.from, params.to,
            params.places, params.bbox, params.coord, params.radius, params.limit, params.offset)
            
        // Factes and heatmaps are optional in JSON response
        val includeFacets = getQueryParam(KEY_FACETS, session.request).map(_.toBoolean).getOrElse(false)
        val includeHeatmap = getQueryParam(KEY_HEATMAP, session.request).map(_.toBoolean).getOrElse(false)
        val extraPayload = Seq(
              { if (includeFacets) Some(Json.toJson(results._2).as[JsObject]) else None },
              { if (includeHeatmap) Some(Json.toJson(results._3).as[JsObject]) else None }).flatten
            
        implicit val verbose = getQueryParam("verbose", session.request).map(_.toBoolean).getOrElse(false)          
        val response =
          extraPayload.foldLeft(Json.toJson(results._1.map(_._1)).as[JsObject])((response, payload) => response ++ payload)
        
        jsonOk(response, session.request)
      }
      
      case Failure(exception) => BadRequest(Json.parse("{ \"error\": \"" + exception.getMessage + "\" }"))
    }
  }

}
