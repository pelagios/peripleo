package controllers.pages

import models._
import play.api.db.slick._
import play.api.mvc.Controller

object DatasetPagesController extends Controller {

  def listAll = DBAction { implicit session =>
    val datasets = Datasets.countAll()
    val things = AnnotatedThings.countAll(true)
    val annotations = Annotations.countAll
    Ok(views.html.datasetList(datasets, things, annotations))
  }
  
  def getDataset(id: String) = DBAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined) {
      val things = AnnotatedThings.countByDataset(dataset.get.id)
      val places = Places.countPlacesInDataset(dataset.get.id)
      val annotations = Annotations.countByDataset(dataset.get.id)
      val supersets = Datasets.getParentHierarchy(dataset.get)
      Ok(views.html.datasetDetails(dataset.get, things, annotations, places, supersets))
    } else {
      NotFound // TODO create decent 'not found' page
    }
  }
  
}