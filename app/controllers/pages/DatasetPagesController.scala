package controllers.pages

import models._
import play.api.db.slick._
import play.api.mvc.Controller

object DatasetPagesController extends Controller {

  def getDataset(id: String) = DBAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined) {
      val things = AnnotatedThings.countByDataset(dataset.get.id)
      val places = Places.countPlacesInDataset(dataset.get.id)
      val annotations = Annotations.countByDataset(dataset.get.id)
      Ok(views.html.dataset(dataset.get, things, places, annotations))
    } else {
      NotFound // TODO create decent 'not found' page
    }
  }
  
}