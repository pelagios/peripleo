package controllers

import controllers.common.JSONWrites._
import global.Global
import index.{ Index, SearchParameters }
import index.places.IndexedPlace
import models.Associations
import models.geo.Gazetteers
import models.core.Dataset
import play.api.mvc.Action
import play.api.db.slick._
import play.api.libs.json.Json
import play.api.Logger

object PlaceController extends AbstractController {
  
  private val SOURCE_DATASET = "source_dataset"
  
  def listGazetteers(limit: Int, offset: Int) = loggingAction { implicit session =>
    jsonOk(Json.toJson(Gazetteers.listAll(offset, limit)), session.request)
  }

  def getGazetteer(gazetteerName: String) = loggingAction { implicit session =>
    Gazetteers.findByNameWithPrefixes(gazetteerName) match {
      case Some(tuple) => jsonOk(Json.toJson(tuple), session.request)
      case _ => NotFound(Json.parse("{ \"message\": \"Gazetteer not found.\" }"))
    }
  }
  
  /**
   * TODO revise!
   */
  def listPlaces(gazetteerName:String, bbox: Option[String], limit: Option[Int], offset: Option[Int]) = loggingAction { implicit session => 
    // Map BBox coordinates
    val bboxTupled = bbox.flatMap(str => {
      val coords = str.split(",").map(_.trim)
      try {
        Some((coords(0).toDouble, coords(1).toDouble, coords(2).toDouble, coords(3).toDouble))
      } catch {
        case _: Throwable => None
      }
    })
    
    val gazetteer = Gazetteers.findByName(gazetteerName) 
    if (gazetteer.isDefined) {
      val allPlaces = Global.index.listAllPlaces(gazetteer.get.name.toLowerCase, bboxTupled, offset.getOrElse(0), limit.getOrElse(20))
      jsonOk(Json.toJson(allPlaces.map(_.asJson)), session.request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Place not found.\" }"))
    }
  } 

  /** Detail information about a place.
    *
    * Includes the cross-gazetteer network graph, and an overview of the data linked
    * to the place.
    */
  def getPlace(uri: String, datasetLimit: Int) = loggingAction { implicit session =>
    val placeNetwork = Global.index.findNetworkByPlaceURI(uri)
    if (placeNetwork.isDefined) {
      val params = SearchParameters.forPlace(uri, 1, 0)
      val (_, facetTree, _, _, _) = Global.index.search(
        params, 
        true,  // facets
        false, // snippets
        false, // time histogram
        0,     // top places
        false, // heatmap
        false) // Only with images
        
      val sourceFacetValues = facetTree.get.getTopChildren(SOURCE_DATASET, datasetLimit)
      val topDatasets = sourceFacetValues.map { case (labelAndId, count) => {
        val split = labelAndId.split('#')
        (split(0), split(1), count)
      }}
 
      jsonOk(Json.toJson((placeNetwork.get, topDatasets)), session.request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
    }
  }
   
}