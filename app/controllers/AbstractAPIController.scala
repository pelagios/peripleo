package controllers

import play.api.mvc.{ Accepting, Controller, RequestHeader }
import play.api.libs.json.{ Json, JsValue }

/** Controller base class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
abstract class AbstractAPIController extends Controller {
  
  private val PRETTY_PRINT = "prettyprint"
    
  private val CALLBACK = "callback"
  
  protected val AcceptsRDFXML = Accepting("application/rdf+xml")
  
  protected val AcceptsTurtle = Accepting("text/turtle")
  
  // TODO implement content negotiation according to the following pattern:
  //    render {
  //      case Accepts.Html() => Ok("") // views.html.list(items))
  //      case Accepts.Json() => Ok("") // Json.toJson(items))
  //      case AcceptsRDFXML => Ok("") 
  //      case AcceptsTurtle => Ok("")
  //    }
  
  /** Helper for creating pretty-printed JSON responses with proper content-type header **/
  protected def jsonOk(obj: JsValue, request: RequestHeader) = {
    val prettyPrint = request.queryString
      .filter(_._1.toLowerCase.equals(PRETTY_PRINT))
      .headOption.flatMap(_._2.headOption)
      .map(_.toBoolean).getOrElse(false)
      
    // TODO wrap response as JSONP if callback parameter is provided
    val callback = request.queryString
      .filter(_._1.toLowerCase.equals(CALLBACK))
      .headOption.flatMap(_._2.headOption)
    
    if (prettyPrint)
      Ok(Json.prettyPrint(obj)).withHeaders(("Content-Type", "application/json; charset=utf-8"))
    else
      Ok(obj)
  }

}