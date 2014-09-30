package controllers

import controllers.common.io.JSONWrites._
import global.Global
import index.{ Index, IndexedObjectTypes }
import models.{ Page, AnnotatedThing, Places }
import play.api.db.slick._
import play.api.libs.json.Json

object SearchController extends AbstractAPIController {

  private val DATASET = "dataset"
  private val ITEM = "item"
  private val PLACE = "place"
  
  /** API search method controller.
    * 
    * @param limit search result page size
    * @param offset search result page offset
    * @query query keyword query
    * @query objectType filter search to a specific object type ('place', 'item' or 'dataset')
    * @query dataset filter search to items in a specific dataset
    * @query places filter search to items referencing specific places 
    */
  def search(limit: Int, offset: Int, query: Option[String], objectType: Option[String], dataset: Option[String], places: Option[String]) = DBAction { implicit session =>
    // Map object types
    val objType = objectType.flatMap(name => name.toLowerCase match {
      case DATASET => Some(IndexedObjectTypes.DATASET)
      case ITEM => Some(IndexedObjectTypes.ANNOTATED_THING)
      case PLACE => Some(IndexedObjectTypes.PLACE)
      case _=> None
    })
    
    // Tokenize and normalize place URIs
    val placeURIs = places.map(_.split(",").map(s => Index.normalizeURI(s.trim())).toSeq).getOrElse(Seq.empty[String])
    
    val results = Global.index.search(limit, offset, query, objType, dataset, placeURIs)
    jsonOk(Json.toJson(results), session.request)
  }

}