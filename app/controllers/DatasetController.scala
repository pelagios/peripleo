package controllers

import models._
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.libs.json.Json
import play.api.libs.json.Writes
import global.Global

object DatasetController extends Controller {
  
  // Implicit JSON serializers
  implicit private val serializeDataset = Json.writes[Dataset]
  implicit private val serializeAnnotatedThing = Json.writes[AnnotatedThing]
  
  implicit private val serializePlacesPerThing = new Writes[Page[(String, Int)]] {
    def writes(page: Page[(String, Int)]) = Json.obj(
      "total" -> page.total,
      "offset" -> page.offset,
      "limit" -> page.limit,
      "items" -> page.items.map { case (gazetteerURI, count) => {
        val centroid = Global.index.findByURI(gazetteerURI).flatMap(_.getCentroid)
        
        Json.obj(
          "gazetteer_uri" -> gazetteerURI,
          "count" -> count,
          "lat" -> centroid.map(_.y),
          "lng" -> centroid.map(_.x)
        )}
      }
    )
  }
  
  def listAll = DBAction { implicit session =>
    Ok(Json.prettyPrint(Json.toJson(Datasets.listAll().items)))
  }
  
  def getDataset(id: String) = DBAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined)
      Ok(Json.prettyPrint(Json.toJson(dataset)))
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
    
  def listAnnotatedThings(id: String) = DBAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined)
      Ok(Json.prettyPrint(Json.toJson(AnnotatedThings.findByDataset(id).items)))
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
  def listPlaces(id: String) = DBAction { implicit session =>
    val places = Places.findPlacesInDataset(id)
    Ok(Json.prettyPrint(Json.toJson(places)))
  } 
  
}