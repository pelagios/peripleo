package controllers.pages

import models._
import play.api.db.slick._
import play.api.mvc.Controller

object AnnotatedThingPagesController extends Controller {
  
  def getAnnotatedThing(id: String) = DBAction { implicit session =>
    val thing = AnnotatedThings.findById(id)
    if (thing.isDefined) {
      val places = Places.countPlacesForThing(id)
      val annotations = Annotations.countByAnnotatedThing(id)
      val datasetHierarchy = Datasets.findAllById(
        thing.get.id +: Datasets.getParentHierarchy(thing.get.dataset))

      Ok("") //views.html.datasetDetails(dataset.get._1, things, annotations, places, supersets, subsets))
    } else {
      NotFound // TODO create decent 'not found' page
    }
  }
  
}
