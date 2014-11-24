package controllers.admin

import models.geo.Gazetteers
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.Logger

object GazetteerAdminController extends Controller with Secured {
  
  def index = adminAction { username => implicit requestWithSession =>
    Ok(views.html.admin.gazetteers(Gazetteers.listAll.map(_._1)))
  }
  
  def deleteGazetteer(name: String) = adminAction { username => implicit requestWithSession =>
    val gazetteer = Gazetteers.findByName(name)
    if (gazetteer.isDefined) {
      Logger.info("Deleting gazetteer: " + name)
      Status(200)
    } else {
      NotFound
    }
  }

}