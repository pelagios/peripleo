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
      val id = dataset.get.id
      val things = AnnotatedThings.countByDataset(id)
      val places = Places.countPlacesInDataset(id)
      val annotations = Annotations.countByDataset(id)
      val supersets = Datasets.findByIds(Datasets.getParentHierarchy(id))
      val subsets = Datasets.listSubsets(id)
      Ok(views.html.datasetDetails(dataset.get, things, annotations, places, supersets, subsets))
    } else {
      NotFound // TODO create decent 'not found' page
    }
  }
  
}
