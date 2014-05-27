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
    val places = 34000 // TODO get stuff in place to compute this number from the gazetteers
    Ok(views.html.home(datasets, items, places))
  }
  
  def search() = DBAction { implicit session =>
    val query = session.request.queryString.get(QUERY).flatMap(_.headOption)
    if (query.isDefined && !query.get.isEmpty) {
      val results = Global.index.search(query.get, 0, Global.DEFAULT_PAGE_SIZE)
      Ok(views.html.searchresults(results))
    } else {
      Redirect(routes.HomepageController.index)
    }
  }

}