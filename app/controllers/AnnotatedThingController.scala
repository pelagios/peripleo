package controllers

import controllers.common.io.JSONWrites._
import global.Global
import index.Index
import models._
import play.api.db.slick._
import play.api.libs.json.{ Json, JsString, Writes }
import index.IndexedObjectTypes
import play.api.Logger

object AnnotatedThingController extends AbstractAPIController {
  
  private val PRETTY_PRINT = "prettyprint"  
  private val CALLBACK = "callback"
    
  def listAll(limit: Int, offset: Int, inDataset: Option[String], forPlaces: Option[String]) = DBAction { implicit session =>
    if (inDataset.isDefined && forPlaces.isDefined) {
      // Filter by place and dataset
      val places = forPlaces.get.split(",").map(s => Index.normalizeURI(s.trim())).toSeq
      val indexHits = Global.index.search(offset, limit, None, Some(IndexedObjectTypes.ANNOTATED_THING), inDataset, places, None, None)
      val annotatedThings = AnnotatedThings.findByIds(indexHits.items.map(_.identifier))
      
      // Note: this will (and should) fail if index and DB are out of sync!
      val result = indexHits.map(idxObj => annotatedThings.find(_.id == idxObj.identifier).get)  
      jsonOk(Json.toJson(result), session.request)
    } else if (inDataset.isDefined) {
      // Filter ONLY by dataset - just redirect to the proper RESTful URL (but keep prettyprint and callback params) 
      val redirectURL = controllers.routes.DatasetController.listAnnotatedThings(inDataset.get, limit, offset).url
      val params = session.request.queryString.filter(param => param._1.toLowerCase == PRETTY_PRINT || param._1.toLowerCase == CALLBACK)
      Redirect(redirectURL, params)
    } else {    
      // List all
      jsonOk(Json.toJson(AnnotatedThings.listAll(false, offset, limit)), session.request)
    }
  }  
  
  def getAnnotatedThing(id: String) = DBAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined)
      jsonOk(Json.toJson(annotatedThing.get), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }  
  
  def listSubItems(id: String, limit: Int, offset: Int) = DBAction { implicit session =>
    val subItems = AnnotatedThings.listChildren(id)
    jsonOk(Json.toJson(subItems), session.request)
  }
  
  def listPlaces(id: String, limit: Int, offset: Int) = DBAction { implicit session =>
    val places = Places.findPlacesForThing(id)
    jsonOk(Json.toJson(places), session.request)
  } 
  
  def listAnnotations(id: String, limit: Int, offset: Int) = DBAction { implicit session =>
    val annotatedThing = AnnotatedThings.findById(id)
    if (annotatedThing.isDefined)
      jsonOk(Json.toJson(Annotations.findByAnnotatedThing(id)), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  } 
  
}
