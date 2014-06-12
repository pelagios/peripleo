package controllers.pages

import models.{ AnnotatedThings, Datasets, Gazetteers }
import global.Global
import play.api.mvc.Controller
import play.api.db.slick._

object HomepageController extends Controller {
  
  private val QUERY = "query"
  
  def index() = DBAction { implicit session =>
    val datasets = Datasets.countAll
    val items = AnnotatedThings.countAll(true)
    val gazetteers = Gazetteers.countAll
    val places = Gazetteers.numDistinctPlaces 
    Ok(views.html.home(datasets, items, gazetteers, places))
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