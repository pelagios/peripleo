package controllers

import play.api.mvc.Controller
import play.api.libs.json.{ Json, JsValue }

/** Controller base class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
abstract class AbstractAPIController extends Controller {
  
  /** Helper for creating pretty-printed JSON responses with proper content-type header **/
  protected def jsonOk(obj: JsValue, prettyPrint: Option[Boolean]) = {
    if (prettyPrint.getOrElse(false))
      Ok(Json.prettyPrint(obj)).withHeaders(("Content-Type", "application/json; charset=utf-8"))
    else
      Ok(obj)
  }

}