package controllers.pages

import play.api.mvc.{ Action, Controller }
import play.api.db.slick._
import global.Global
import play.api.Logger

object HomepageController extends Controller {
  
  private val QUERY = "query"
  
  def index() = Action {
    Ok(views.html.home(1000, 1000, 1000))
  }
  
  def search() = DBAction { implicit session =>
    val queryString = session.request.queryString.get(QUERY).flatMap(_.headOption)
    if (queryString.isDefined) {
      val results = Global.index.search(queryString.get, 50)
      results.items.foreach(result => Logger.info(result.title))
      Ok("")
    } else {
      BadRequest
    }
  }

}