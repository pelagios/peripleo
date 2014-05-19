package controllers.labs

import play.api.mvc.{ Action, Controller }

object Heatmap extends Controller {
  
  def index(dataset: Option[String], annotatedThing: Option[String]) = Action {
    Ok(views.html.heatmap(dataset, annotatedThing)) 
  }

}