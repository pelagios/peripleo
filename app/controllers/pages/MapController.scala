package controllers.pages

import controllers.common.JSONWrites._
import models.MasterHeatmap
import play.api.libs.json.Json
import play.api.db.slick._
import controllers.AbstractController

object MapController extends AbstractController {
  
  def index = loggingAction { implicit session => 
    Ok(views.html.masterHeatmap())
  }

  def masterHeatmap = loggingAction { implicit session => 
    jsonOk(Json.toJson(MasterHeatmap.listAll), session.request)
  }
  
}