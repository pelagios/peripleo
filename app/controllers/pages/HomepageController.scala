package controllers.pages

import play.api.mvc.{ Action, Controller }
import play.api.db.slick._
import global.Global
import play.api.Logger

object HomepageController extends Controller {
  
  def index() = Action {
    Ok(views.html.home())
  }
  
  def search(query: String) = DBAction { implicit session =>
    val results = Global.index.search(query, 50)
    results.items.foreach(result => Logger.info(result.title))
    Ok("")
  }

}