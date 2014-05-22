package controllers.pages

import models._
import play.api.db.slick._
import play.api.mvc.Controller

object DatasetPagesController extends Controller {

  def getDataset(id: String) = DBAction { implicit session =>
    val dataset = Datasets.findById(id)
    if (dataset.isDefined)
      Ok(views.html.dataset(dataset.get))
    else
      NotFound // TODO create decent 'not found' page
  }
  
}