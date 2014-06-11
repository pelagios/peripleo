package controllers.pages

import global.Global
import index.Index
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.Logger

object PlacePagesController extends Controller {
  
  def getPlace(uri: String) = DBAction { implicit session =>
    val place = Global.index.findPlaceByURI(Index.normalizeURI(uri))
    if (place.isDefined) {
      val network = Global.index.getNetwork(place.get)
      Ok(views.html.placeDetails(place.get, network))
    } else {
      NotFound // TODO create decent 'not found' page
    }
  }

}