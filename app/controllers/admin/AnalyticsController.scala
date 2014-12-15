package controllers.admin

import models.AccessLog
import play.api.db.slick._
import play.api.mvc.Controller

object AnalyticsController extends Controller with Secured {
  
  def index() = adminAction { username => implicit requestWithSession =>
    Ok(views.html.admin.accessLog(AccessLog.listAll))
  }

}