package controllers.common.io

import global.Global
import index.places.IndexedPlace
import models._
import play.api.db.slick._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import index.IndexedObject

/** JSON writers for model classes.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object JSONWrites {
  
  /** Writes a place **/
  implicit val placeWrites: Writes[IndexedPlace] = (
    (JsPath \ "gazetteer_uri").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "place_category").writeNullable[String] ~
    (JsPath \ "names").write[Seq[String]] ~
    (JsPath \ "description").writeNullable[String] ~
    (JsPath \ "centroid_lat").writeNullable[Double] ~
    (JsPath \ "centroid_lng").writeNullable[Double]
  )(place => (
      place.uri,
      place.title,
      place.category.map(_.toString),
      place.names.map(_.chars),
      place.description,
      place.geometry.map(_.centroid.y),
      place.geometry.map(_.centroid.x)))  
  
  /** Writes a search result item **/
  implicit val indexedObjectWrites: Writes[IndexedObject] = (
      (JsPath \ "identifier").write[String] ~
      (JsPath \ "title").write[String] ~
      (JsPath \ "description").writeNullable[String] ~
      (JsPath \ "object_type").write[String]
  )(obj => (
      obj.identifier,
      obj.title,
      obj.description,
      obj.objectType.toString))    
      
  /** Writes a Gazetteer URI, with place data pulled from the index on the fly **/
  implicit def gazetteerURIWrites(implicit verbose: Boolean = true): Writes[GazetteerReference] = (
    (JsPath \ "gazetteer_uri").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "centroid_lat").writeNullable[Double] ~
    (JsPath \ "centroid_lng").writeNullable[Double] ~ 
    (JsPath).writeNullable[IndexedPlace]
  )(place => {
      val centroid = place.geometry.map(_.centroid)
      (place.uri,
       place.title,
       centroid.map(_.y),
       centroid.map(_.x),
       { if (verbose) Global.index.findPlaceByURI(place.uri) else None })}) 

      
  /** Writes a pair (Place, Occurrence-Count) **/
  implicit def placeCountWrites(implicit verbose: Boolean = true): Writes[(GazetteerReference, Int)] = (
      (JsPath).write[GazetteerReference] ~
      (JsPath \ "number_of_occurrences").write[Int]
  )(t  => (t._1, t._2))   
       
  
  /** Writes a dataset, with annotation count and place count pulled from the DB on the fly **/
  implicit def datasetWrites(implicit s: Session): Writes[Dataset] = (
    (JsPath \ "id").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "publisher").write[String] ~
    (JsPath \ "description").writeNullable[String] ~
    (JsPath \ "license").write[String] ~
    (JsPath \ "homepage").writeNullable[String] ~
    (JsPath \ "created_at").write[Long] ~
    (JsPath \ "modified_at").write[Long] ~
    (JsPath \ "void_url").writeNullable[String] ~
    (JsPath \ "datadump_url").writeNullable[String] ~
    (JsPath \ "number_of_items").write[Int] ~
    (JsPath \ "number_of_annotations").write[Int]  ~
    (JsPath \ "number_of_unique_places").write[Int]
  )(dataset => (
      dataset.id,
      dataset.title,
      dataset.publisher,
      dataset.description,
      dataset.license,
      dataset.homepage,
      dataset.created.getTime,
      dataset.modified.getTime,
      dataset.voidURI,
      dataset.datadump,
      AnnotatedThings.countByDataset(dataset.id),
      Annotations.countByDataset(dataset.id),
      Places.countPlacesInDataset(dataset.id)))

      
  /** Writes an annotated thing, with annotation count and place count pulled from the DB on the fly **/ 
  implicit def annotatedThingWrites(implicit s: Session): Writes[AnnotatedThing] = (
    (JsPath \ "id").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "in_dataset").write[String] ~
    (JsPath \ "is_part_of").writeNullable[String] ~
    (JsPath \ "homepage").writeNullable[String] ~
    (JsPath \ "number_of_subitems").writeNullable[Int] ~
    (JsPath \ "number_of_annotations").write[Int] ~ 
    (JsPath \ "number_of_unique_places").write[Int]
  )(thing => (
      thing.id,
      thing.title,
      thing.dataset,
      thing.isPartOf,
      thing.homepage,
      { val count = AnnotatedThings.countChildren(thing.id); if (count > 0) Some(count) else None },
      Annotations.countByAnnotatedThing(thing.id),
      Places.countPlacesForThing(thing.id)))
  
      
  /** Writes an annotation **/
  implicit val annotationWrites: Writes[Annotation] = (
    (JsPath \ "uuid").write[String] ~
    (JsPath \ "in_dataset").write[String] ~
    (JsPath \ "annotated_item").write[String] ~
    (JsPath \ "place_uri").write[String]
  )(a => (
      a.uuid.toString,
      a.dataset,
      a.annotatedThing,
      a.gazetteerURI))
      
      
  /** Writes a page of items **/
  implicit def pageWrites[A](implicit fmt: Writes[A]): Writes[Page[A]] = (
    (JsPath \ "total").write[Long] ~
    (JsPath \ "offset").writeNullable[Int] ~
    (JsPath \ "limit").writeNullable[Long] ~
    (JsPath \ "items").write[Seq[A]]
  )(page => (
      page.total, 
      { if (page.offset > 0) Some(page.offset) else None },
      { if (page.limit < Int.MaxValue) Some(page.limit) else None },
      page.items))

}