package controllers

import com.vividsolutions.jts.geom.Coordinate
import index.{ Index, SearchParameters }
import index.objects.IndexedObjectTypes
import java.util.UUID
import java.sql.Timestamp
import models.{ AccessLog, AccessLogRecord }
import models.geo.BoundingBox
import play.api.Play
import play.api.Logger
import play.api.db.slick._
import play.api.mvc.{ Accepting, AnyContent, BodyParsers, Controller, RequestHeader, SimpleResult }
import play.api.libs.json.{ Json, JsValue }
import scala.util.{ Try, Success, Failure }
import index.DateFilterMode

/** Controller base class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
abstract class AbstractController extends Controller {   
    
  /** Protected constants **/
  
  protected val KEY_QUERY = "query"
  protected val KEY_OBJECT_TYPE = "types"
  protected val KEY_EXCLUDE_OBJECT_TYPES = "exclude_types"
  protected val KEY_DATASETS = "datasets"
  protected val KEY_EXCLUDE_DATASETS = "exclude_datasets"
  protected val KEY_GAZETTEERS = "gazetteers"
  protected val KEY_EXCLUDE_GAZETTEERS = "exclude_gazetteers"
  protected val KEY_LANGUAGES = "lang"
  protected val KEY_EXCLUDE_LANGUAGES = "exclude_lang"
  protected val KEY_FROM = "from"
  protected val KEY_TO = "to"
  protected val KEY_DATE_FILTER_MODE = "date_filter"
  protected val KEY_PLACES = "places"
  protected val KEY_BBOX = "bbox"
  protected val KEY_LON = "lon"
  protected val KEY_LAT = "lat"
  protected val KEY_RADIUS = "radius"
  protected val KEY_LIMIT = "limit"
  protected val KEY_OFFSET = "offset"
  
  protected val AcceptsRDFXML = Accepting("application/rdf+xml")
  protected val AcceptsTurtle = Accepting("text/turtle")
        
  
  /** Private constants **/
  
  private val ITEM = "item"
  private val PLACE = "place"
  private val DATASET = "dataset"
  
  private val CORS_ENABLED = Play.current.configuration.getBoolean("peripleo.enable.cors").getOrElse(false)
  private val PRETTY_PRINT = "prettyprint"
  private val CALLBACK = "callback"
  private val HEADER_USERAGENT = "User-Agent"
  private val HEADER_REFERER = "Referer"
  private val HEADER_ACCEPT = "Accept"
  
  private val CONTAINS = "contains"
  private val INTERSECTS = "intersects"
  
  
  /** Helper to grab a parameter value from the query string **/
  protected def getQueryParam(key: String, request: RequestHeader): Option[String] = 
    request.queryString
      .filter(_._1.equalsIgnoreCase(key))
      .headOption.flatMap(_._2.headOption)
  
  /** Helper to grab a parameter that's a list of comma-separated values as a Seq[String] **/ 
  protected def getQueryParamList(key: String, request: RequestHeader): Seq[String] =
    getQueryParam(key, request).map(_.split(",").toSeq.map(_.trim)).getOrElse(Seq.empty[String])
  
  /** Shorthand for parsing a parameter that's a list of object types **/
  protected def getQueryParamAsListOfTypes(key: String, request: RequestHeader) =
    getQueryParamList(key, request).flatMap(_.toLowerCase match {
      case DATASET => Some(IndexedObjectTypes.DATASET)
      case ITEM => Some(IndexedObjectTypes.ANNOTATED_THING)
      case PLACE => Some(IndexedObjectTypes.PLACE)
      case _=> None          
    })    
    
  /** Helper methods that parses all search paramters from the query string **/
  protected def parseSearchParams(request: RequestHeader): Try[SearchParameters] = {
    try {
      val query = 
        getQueryParam(KEY_QUERY, request)
      
      val objectTypes = getQueryParamAsListOfTypes(KEY_OBJECT_TYPE, request)

      val excludeObjectTypes = getQueryParamAsListOfTypes(KEY_EXCLUDE_OBJECT_TYPES, request)        
      
      val datasets = getQueryParamList(KEY_DATASETS, request)
        
      val excludeDatasets = getQueryParamList(KEY_EXCLUDE_DATASETS, request)
        
      val gazetteers = getQueryParamList(KEY_GAZETTEERS, request)
        
      val excludeGazetteers = getQueryParamList(KEY_EXCLUDE_GAZETTEERS, request)
      
      val languages = getQueryParamList(KEY_LANGUAGES, request)
      
      val excludeLanguages = getQueryParamList(KEY_EXCLUDE_LANGUAGES, request)
        
      val fromYear =
        getQueryParam(KEY_FROM, request).map(_.toInt)
        
      val toYear =
        getQueryParam(KEY_TO, request).map(_.toInt)
        
      val dateFilterMode =
        getQueryParam(KEY_DATE_FILTER_MODE, request)
          .map(_.toLowerCase match {
            case CONTAINS => DateFilterMode.CONTAINS
            case INTERSECTS => DateFilterMode.INTERSECTS
          }).getOrElse(DateFilterMode.INTERSECTS)
        
      val places =
        getQueryParam(KEY_PLACES, request)
          .map(_.split(",").toSeq.map(uri => Index.normalizeURI(uri.trim)))
          .getOrElse(Seq.empty[String])      
      
      val bbox = 
        getQueryParam(KEY_BBOX, request).flatMap(BoundingBox.fromString(_))
       
      val coord = {
        val lon: Option[Double] = getQueryParam(KEY_LON, request).map(_.toDouble)
        val lat: Option[Double] = getQueryParam(KEY_LAT, request).map(_.toDouble)
        if (lon.isDefined && lat.isDefined)
          Some(new Coordinate(lon.get, lat.get))
        else 
          None
      } 

      val radius = 
        getQueryParam(KEY_RADIUS, request).map(_.toDouble)
        
      val limit =
        getQueryParam(KEY_LIMIT, request).map(_.toInt).getOrElse(20)
      
      val offset =
        getQueryParam(KEY_OFFSET, request).map(_.toInt).getOrElse(0)
    
      val params = 
        SearchParameters(query, objectTypes, excludeObjectTypes, datasets, excludeDatasets, gazetteers, 
          excludeGazetteers, languages, excludeLanguages, fromYear, toYear, dateFilterMode, places, bbox, 
          coord, radius, limit, offset)
          
      if (params.isValid)
        Success(params)
      else 
        Failure(new RuntimeException("Invalid query: " + params.error.get))
    } catch {
      // TODO extend error handling, so we can give detailes on which parameter was wrong
      case t: Throwable => Failure(new RuntimeException("Invalid query parameters"))
    }
  }
  
  private def isNoBot(userAgent: String): Boolean = {
    // TODO add some basic rules to filter out at least Google and Twitter
    true
  }
  
  
  /** A wrapper around DBAction that provides analytics logging **/
  def loggingAction(f: DBSessionRequest[AnyContent] => SimpleResult) = {
    DBAction(BodyParsers.parse.anyContent)(implicit rs => {
      val startTime = System.currentTimeMillis
      
      // Execute controller
      val result = f(rs) 
 
      val headers = rs.request.headers
      val userAgent = headers.get(HEADER_USERAGENT).getOrElse("undefined")      
 
      if (isNoBot(userAgent)) {
        val uri = rs.request.uri
        val ip = rs.request.remoteAddress
        val referrer = headers.get(HEADER_REFERER)
        val accept = headers.get(HEADER_ACCEPT)
        
        AccessLog.insert(AccessLogRecord(UUID.randomUUID, 
          new Timestamp(startTime), 
          uri,
          ip,
          userAgent,
          referrer,
          accept, 
          { System.currentTimeMillis - startTime }.toInt))
      }
      
      result
    })
  }
    
  
  // TODO implement content negotiation according to the following pattern:
  //    render {
  //      case Accepts.Html() => Ok("") // views.html.list(items))
  //      case Accepts.Json() => Ok("") // Json.toJson(items))
  //      case AcceptsRDFXML => Ok("") 
  //      case AcceptsTurtle => Ok("")
  //    }
  
  
  /** Helper for creating pretty-printed JSON responses with proper content-type header **/
  protected def jsonOk(obj: JsValue, request: RequestHeader) = {
    val prettyPrint = getQueryParam(PRETTY_PRINT, request).map(_.toBoolean).getOrElse(false)
    val callback = getQueryParam(CALLBACK, request)
    
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

