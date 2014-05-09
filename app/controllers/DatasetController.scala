package controllers

import models.{ Dataset, Datasets }
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.libs.json.Json

object DatasetController extends Controller {
  
  implicit private val jsonWrite = Json.writes[Dataset]
  
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
    Ok(Json.parse("{ \"message\": \"Hello World!\" }"))
  }
  
}