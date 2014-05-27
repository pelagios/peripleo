package controllers

import controllers.common.io.JSONWrites._
import java.util.UUID
import models.Annotations
import play.api.db.slick._
import play.api.libs.json.Json

object AnnotationController extends AbstractAPIController {

  def listAll(prettyPrint: Boolean) = DBAction { implicit session =>
    jsonOk(Json.toJson(Annotations.listAll()), prettyPrint)
  }
  
  def getAnnotation(id: UUID, prettyPrint: Boolean) = DBAction { implicit session =>
    val annotation = Annotations.findByUUID(id)
    if (annotation.isDefined)
      jsonOk(Json.toJson(annotation.get), prettyPrint)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
}