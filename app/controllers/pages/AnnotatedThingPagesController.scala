package controllers.pages

import models.Associations
import models.core.{ AnnotatedThings, Datasets, Images }
import play.api.db.slick._
import play.api.mvc.Controller
import controllers.AbstractController

object AnnotatedThingPagesController extends AbstractController {
  
  def getAnnotatedThing(id: String) = loggingAction { implicit session =>
    val thing = AnnotatedThings.findById(id)
    if (thing.isDefined) {
      val images = Images.findByAnnotatedThing(id)
      val places = Associations.countPlacesForThing(id)
      val datasetHierarchy = Datasets.findByIds(thing.get.dataset +: Datasets.getParentHierarchy(thing.get.dataset)).reverse
      Ok(views.html.annotatedThingDetails(thing.get, images, datasetHierarchy))
    } else {
      NotFound
    }
  }
  
  def getAdjacencyGraph(id: String) = loggingAction { implicit session =>
    Ok(views.html.placeAdjacencyHack(id))
  }
  
}
