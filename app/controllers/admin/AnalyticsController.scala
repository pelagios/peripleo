package controllers.admin

import models.AccessLog
import play.api.db.slick._
import play.api.mvc.Controller
import models.AccessLogAnalytics

object AnalyticsController extends Controller with Secured {
  
  def index() = adminAction { username => implicit requestWithSession =>
    // TODO only a temporary hack!
    val allLogRecords = AccessLog.listAll()
    Ok(views.html.admin.accessLog(new AccessLogAnalytics(allLogRecords), allLogRecords.take(30)))
  }

}