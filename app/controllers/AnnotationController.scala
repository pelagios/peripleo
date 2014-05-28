package controllers

import controllers.common.io.JSONWrites._
import java.util.UUID
import models.Annotations
import play.api.db.slick._
import play.api.libs.json.Json

object AnnotationController extends AbstractAPIController {

  def listAll(offset: Int, limit: Int) = DBAction { implicit session =>
    jsonOk(Json.toJson(Annotations.listAll()), session.request)
  }
  
  def getAnnotation(id: UUID) = DBAction { implicit session =>
    val annotation = Annotations.findByUUID(id)
    if (annotation.isDefined)
      jsonOk(Json.toJson(annotation.get), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
}