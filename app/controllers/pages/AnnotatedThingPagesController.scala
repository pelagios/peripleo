package controllers.pages

import models.Associations
import models.core.{ AnnotatedThings, Datasets, Images }
import play.api.db.slick._
import play.api.mvc.Controller
import controllers.AbstractAPIController

object AnnotatedThingPagesController extends AbstractAPIController {
  
  def getAnnotatedThing(id: String) = loggingAction { implicit session =>
    val thing = AnnotatedThings.findById(id)
    if (thing.isDefined) {
      val thumbnails = Images.findByAnnotatedThing(id)._1
      val places = Associations.countPlacesForThing(id)
      val datasetHierarchy = Datasets.findByIds(thing.get.dataset +: Datasets.getParentHierarchy(thing.get.dataset)).reverse
      Ok(views.html.annotatedThingDetails(thing.get, thumbnails, datasetHierarchy))
    } else {
      NotFound
    }
  }
  
}
