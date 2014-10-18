package controllers

import controllers.common.io.JSONWrites._
import global.Global
import index.{ Index, IndexedObjectTypes }
import play.api.db.slick._
import play.api.libs.json.Json

object SearchController extends AbstractAPIController {

  private val ITEM = "item"
  private val PLACE = "place"
  private val DATASET = "dataset"
    
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
  def search(limit: Int, offset: Int, query: Option[String], objectType: Option[String], dataset: Option[String], 
    places: Option[String], yearFrom: Option[Int], yearTo: Option[Int]) = DBAction { implicit session =>
        
    // Map object types
    val objType = objectType.flatMap(name => name.toLowerCase match {
      case DATASET => Some(IndexedObjectTypes.DATASET)
      case ITEM => Some(IndexedObjectTypes.ANNOTATED_THING)
      case PLACE => Some(IndexedObjectTypes.PLACE)
      case _=> None
    })
    
    // Tokenize and normalize place URIs
    val placeURIs = places.map(_.split(",").map(s => Index.normalizeURI(s.trim())).toSeq).getOrElse(Seq.empty[String])
            
    val results = Global.index.search(limit, offset, query, objType, dataset, placeURIs, yearFrom, yearTo)
    jsonOk(Json.toJson(results), session.request)
  }

}