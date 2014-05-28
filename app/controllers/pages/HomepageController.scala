package controllers.pages

import models.{ AnnotatedThings, Datasets }
import global.Global
import play.api.mvc.Controller
import play.api.db.slick._

object HomepageController extends Controller {
  
  private val QUERY = "query"
  
  def index() = DBAction { implicit session =>
    val datasets = Datasets.countAll
    val items = AnnotatedThings.countAll
    val places = 36221 // TODO get rid of this hard-coded number and replace with live count from gazetteers
    Ok(views.html.home(datasets, items, places))
  }
  
  // TODO implement search entirely via the API
  def search() = DBAction { implicit session =>
    val query = session.request.queryString.get(QUERY).flatMap(_.headOption)
    if (query.isDefined && !query.get.isEmpty) {
      val results = Global.index.search(query.get, 0, 20)
      Ok(views.html.searchResults(results))
    } else {
      Redirect(routes.HomepageController.index)
    }
  }

}