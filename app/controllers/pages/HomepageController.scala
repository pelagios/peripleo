package controllers.pages

import models.core.{ AnnotatedThings, Datasets }
import models.geo.Gazetteers
import global.Global
import play.api.mvc.{ Action, Controller }
import play.api.db.slick._
import play.api.Logger
import index.objects.IndexedObjectTypes
import index.Index
import controllers.AbstractController

object HomepageController extends AbstractController {
  
  private val DATASET = "dataset"
  private val ITEM = "item"
  private val PLACE = "place"
  
  def index() = loggingAction { implicit session =>
    val datasets = Datasets.countAll()
    val items = AnnotatedThings.countAll(true)
    val gazetteers = Gazetteers.countAll
    val places = Global.index.numPlaceNetworks
    Ok(views.html.home(datasets, items, gazetteers, places))
  }
  
  def search(limit: Int, offset: Int, query: Option[String], objectType: Option[String], dataset: Option[String], 
      places: Option[String], yearFrom: Option[Int], yearTo: Option[Int]) = loggingAction { implicit session =>
        
    val startTime = System.currentTimeMillis
    // Map object types
    val objType = objectType.flatMap(name => name.toLowerCase match {
      case DATASET => Some(IndexedObjectTypes.DATASET)
      case ITEM => Some(IndexedObjectTypes.ANNOTATED_THING)
      case PLACE => Some(IndexedObjectTypes.PLACE)
      case _=> None
    })
    
    // Tokenize and normalize place URIs
    val placeURIs = places.map(_.split(",").map(s => Index.normalizeURI(s.trim())).toSeq).getOrElse(Seq.empty[String])
    
    val results = Global.index.search(limit, offset, query, objType, dataset, placeURIs, yearFrom, yearTo, None, None, None)
    
    Ok(views.html.searchResults(results._1, (System.currentTimeMillis - startTime)))
  }
  
  def defineAdvancedSearch() = Action {
    Ok(views.html.advancedSearch())
  }

}