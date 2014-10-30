package controllers

import java.sql.Timestamp
import models.{ AccessLog, LogRecord }
import play.api.Play
import play.api.Logger
import play.api.db.slick._
import play.api.mvc.{ Accepting, AnyContent, BodyParsers, Controller, RequestHeader, SimpleResult }
import play.api.libs.json.{ Json, JsValue }

/** Controller base class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
abstract class AbstractAPIController extends Controller {
  
  private val CORS_ENABLED = Play.current.configuration.getBoolean("api.enable.cors").getOrElse(false)
  
  private val PRETTY_PRINT = "prettyprint"
    
  private val CALLBACK = "callback"
  
  private val HEADER_USERAGENT = "User-Agent"
  
  private val HEADER_REFERER = "Referer"
  
  private val HEADER_ACCEPT = "Accept"
  
  protected val AcceptsRDFXML = Accepting("application/rdf+xml")
  
  protected val AcceptsTurtle = Accepting("text/turtle")
  
  def loggingAction(f: DBSessionRequest[AnyContent] => SimpleResult) = {
    DBAction(BodyParsers.parse.anyContent)(rs => { 
      val startTime = System.currentTimeMillis
      
      // TODO extract loggable parameters from request
      
      val result = f(rs) 
      Logger.info("Took " + (System.currentTimeMillis - startTime) + "ms")
      
      // TODO write to DB
      
      result
    })
  }
  
  /*
  protected def logAccess(implicit request: RequestHeader, session: Session) = {
    val headers = request.headers
    
    AccessLog.insert(LogRecord(
      None, 
      new Timestamp(System.currentTimeMillis),
      request.uri, 
      request.remoteAddress,
      headers.get(HEADER_USERAGENT).getOrElse("undefined"),
      headers.get(HEADER_REFERER),
      headers.get(HEADER_ACCEPT)))
  }
  */
  
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
      
    val callback = request.queryString
      .filter(_._1.toLowerCase.equals(CALLBACK))
      .headOption.flatMap(_._2.headOption)
    
    val headers = if (CORS_ENABLED) Seq(("Access-Control-Allow-Origin" -> "*")) else Seq.empty[(String, String)]
    if (callback.isDefined) {
      val json =
        if (prettyPrint) 
          Json.prettyPrint(obj)
        else
          Json.stringify(obj)
  
      Ok(callback.get + "(" + json + ");").withHeaders({ ("Content-Type", "application/javascript") +: headers }:_*)
    } else {
      if (prettyPrint)
        Ok(Json.prettyPrint(obj)).withHeaders({ ("Content-Type", "application/json; charset=utf-8") +: headers }:_*)
      else
        Ok(obj).withHeaders(headers:_*)
    }
  }

}

