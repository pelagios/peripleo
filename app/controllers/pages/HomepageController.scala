package controllers.pages

import models.{ AnnotatedThings, Datasets, Gazetteers }
import global.Global
import play.api.mvc.Controller
import play.api.db.slick._
import play.api.Logger

object HomepageController extends Controller {
  
  private val QUERY = "query"
  
  def index() = DBAction { implicit session =>
    val datasets = Datasets.countAll()
    val items = AnnotatedThings.countAll(true)
    val gazetteers = Gazetteers.countAll
    val places = Global.index.numPlaceNetworks
    Ok(views.html.home(datasets, items, gazetteers, places))
  }
  
  def search(query: String, limit: Int, offset: Int) = DBAction { implicit session =>
    val results = Global.index.search(query, offset, limit)
    Ok(views.html.searchResults(results))
  }

}