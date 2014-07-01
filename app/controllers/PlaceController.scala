package controllers

import controllers.common.io.JSONWrites._
import global.Global
import index.places.IndexedPlace
import models.{ Dataset, Places }
import play.api.mvc.Action
import play.api.db.slick._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object PlaceController extends AbstractAPIController {

  def listAll(limit: Int, offset: Int) = Action { implicit request => 
    jsonOk(Json.parse("{ \"message\": \"Hello World\" }"), request)
  }  
  
  def getPlace(uri: String) = Action { implicit request =>
    val place = Global.index.findPlaceByURI(uri)
    if (place.isDefined)
      jsonOk(Json.toJson(place.get), request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
  def getNetwork(uri: String) = Action { implicit request =>
    val network = Global.index.findNetworkByPlaceURI(uri)
    if(network.isDefined) {
      jsonOk(Json.toJson(network), request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
    }
  }
  
  def listReferences(uri: String, includeCloseMatches: Boolean) = DBAction { implicit request =>
    val place = Global.index.findPlaceByURI(uri)
    if (place.isDefined) {
      val asTuples = Places.findDatasetsForPlace(place.get.uri).map { case (dataset, occurences) => {
        val numReferencingItems = Places.countThingsForPlaceAndDataset(place.get.uri, dataset.id)
        (dataset, References(place.get, occurences, numReferencingItems))
      }}
      
      val grouped = asTuples.groupBy(_._1).mapValues(_.map(_._2)).toSeq
      implicit val verbose = false
      jsonOk(Json.toJson(grouped), request.request)
    } else {
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
    }
  }
  
}

case class References(toPlace: IndexedPlace, occurrencesInDataset: Int, numReferencingItems: Int)

object References {
  
  implicit def referencesInsideDatasetWrites: Writes[References] = (
    (JsPath \ "to_place").write[IndexedPlace] ~
    (JsPath \ "num_occurrences").write[Int] ~
    (JsPath \ "num_referencing_items").write[Int]
  )(r => (r.toPlace, r.occurrencesInDataset, r.numReferencingItems))
  
  implicit def referencesWrites(implicit s: Session): Writes[(Dataset, Seq[References])] = (
    (JsPath \ "dataset").write[Dataset] ~
    (JsPath \ "references").write[Seq[References]]
  )(t => (t._1, t._2)) 
  
}