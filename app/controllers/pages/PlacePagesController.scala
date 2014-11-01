package controllers.pages

import global.Global
import index.Index
import models.geo.Gazetteers
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import controllers.AbstractController

object PlacePagesController extends AbstractController {
  
  def getPlace(uri: String) = loggingAction { implicit session =>
    val network = Global.index.findNetworkByPlaceURI(Index.normalizeURI(uri))
    if (network.isDefined) {
      Ok(views.html.placeDetails(network.flatMap(_.getPlace(uri)).get, network.get))
    } else {
      NotFound
    }
  }
  
  def listGazetteers() = DBAction { implicit session =>
    // TODO implement
    Ok("")
  }
  
  def showGazetteer(name: String) = Action { 
    Ok(views.html.showGazetteer(name))    
  }

}