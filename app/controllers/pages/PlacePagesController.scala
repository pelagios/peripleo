package controllers.pages

import global.Global
import index.Index
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.Logger
import play.api.libs.json.Json

object PlacePagesController extends Controller {
  
  def getPlace(uri: String) = DBAction { implicit session =>
    val network = Global.index.findNetworkByPlaceURI(Index.normalizeURI(uri))
    if (network.isDefined) {
      Ok(views.html.placeDetails(network.flatMap(_.getPlace(uri)).get, network.get))
    } else {
      NotFound(Json.parse("{ \"message\": \"Place not found.\" }")) // TODO create decent 'not found' page
    }
  }

}