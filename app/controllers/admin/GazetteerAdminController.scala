package controllers.admin

import models.geo.Gazetteers
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.Logger
import global.Global

object GazetteerAdminController extends Controller with Secured {
  
  def index = adminAction { username => implicit requestWithSession =>
    Ok(views.html.admin.gazetteers(Gazetteers.listAll.map(_._1)))
  }
  
  def deleteGazetteer(name: String) = adminAction { username => implicit requestWithSession =>
    val gazetteer = Gazetteers.findByName(name)
    if (gazetteer.isDefined) {
      Logger.info("Deleting gazetteer: " + name)
      
      Gazetteers.delete(gazetteer.get.name)
      
      Global.index.deleteGazetter(gazetteer.get.name)
      Logger.info("Done.")
      Status(200)
    } else {
      NotFound
    }
  }

}