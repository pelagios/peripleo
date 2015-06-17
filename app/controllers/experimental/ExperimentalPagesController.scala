package controllers.experimental

import controllers.AbstractController
import global.Global
import models.Associations
import play.api.db.slick._

object ExperimentalPagesController extends AbstractController {
  
  def getAdjacencyGraph(id: String) = loggingAction { implicit session =>
    Ok(views.html.placeAdjacencyHack(id))
  }
  
  def listItemVectors(limit: Int, offset: Int) = DBAction { implicit session =>
    // val places = Global.index.listAllPlaceNetworks(offset, limit).flatMap(_.places).map(p => (p.label, p.uri))
    val vectors = Associations.findThingVectorsForPlaces()
    val response = vectors.keySet.map(uri => {
      val place = Global.index.findPlaceByURI(uri)
    
      uri + ";" +
        place.map(_.label).getOrElse("?") + ";" +
        vectors.get(uri).map(_.mkString(",")).getOrElse("")
    }).mkString("\n")
    
    Ok(response)
  }
  
}