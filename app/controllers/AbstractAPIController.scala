package controllers

import play.api.mvc.Controller
import play.api.libs.json.{ Json, JsValue }

abstract class AbstractAPIController extends Controller {
  
  protected def jsonOk(obj: JsValue, prettyPrint: Option[Boolean]) = {
    if (prettyPrint.getOrElse(false))
      Ok(Json.prettyPrint(obj)).withHeaders(("Content-Type", "application/json; charset=utf-8"))
    else
      Ok(obj)
  }

}