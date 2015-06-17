package controllers.pages

import models.core.{ AnnotatedThings, Datasets }
import models.geo.Gazetteers
import global.Global
import play.api.mvc.{ Action, Controller }
import play.api.db.slick._
import play.api.Logger
import index.objects.IndexedObjectTypes
import index.Index
import controllers.AbstractController

object LandingPageController extends AbstractController {
  
  def index() = loggingAction { implicit session =>
    // Placeholder for a future landing page - for now we just redirect to the map
    Redirect(controllers.pages.routes.LandingPageController.map())
  }
  
  def map() = loggingAction { implicit session =>
    Ok(views.html.browsableMap())
  }

}
