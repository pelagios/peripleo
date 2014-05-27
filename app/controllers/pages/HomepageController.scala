package controllers.pages

import play.api.mvc.{ Action, Controller }
import play.api.db.slick._
import global.Global

object HomepageController extends Controller {
  
  private val QUERY = "query"
  
  def index() = Action {
    Ok(views.html.home(1000, 1000, 1000))
  }
  
  def search() = DBAction { implicit session =>
    val query = session.request.queryString.get(QUERY).flatMap(_.headOption)
    if (query.isDefined && !query.get.isEmpty) {
      val results = Global.index.search(query.get, Global.DEFAULT_PAGE_SIZE)
      Ok(views.html.searchresults(results))
    } else {
      Redirect(routes.HomepageController.index)
    }
  }

}