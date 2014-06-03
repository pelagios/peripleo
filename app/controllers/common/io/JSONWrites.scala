package controllers.common.io

import global.Global
import global.index.IndexedObject
import java.sql.Date
import models._
import org.pelagios.api.gazetteer.Place
import play.api.db.slick._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/** JSON writers for model classes.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object JSONWrites {
  
  /** Writes a place **/
  implicit val placeWrites: Writes[Place] = (
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
      place.descriptions.headOption.map(literal => literal.chars + literal.lang.map("@" + _).getOrElse("")),
      place.getCentroid.map(_.y),
      place.getCentroid.map(_.x)))
  
      
  /** Writes a Gazetteer URI, with place data pulled from the index on the fly **/
  implicit def gazetteerURIWrites(implicit verbose: Boolean = true): Writes[GazetteerURI] = (
    (JsPath \ "gazetteer_uri").write[String] ~
    (JsPath).writeNullable[Place]
  )(uri => (
      uri.uri,
      { if (verbose) Global.gazetteer.findByURI(uri.uri) else None })) 

      
  /** Writes a pair (Place, Occurrence-Count) **/
  implicit def placeCountWrites(implicit verbose: Boolean = true): Writes[(GazetteerURI, Int)] = (
      (JsPath).write[GazetteerURI] ~
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
    (JsPath \ "number_of_annotations").write[Int] ~ 
    (JsPath \ "number_of_unique_places").write[Int]
  )(thing => (
      thing.id,
      thing.title,
      thing.dataset,
      thing.isPartOf,
      thing.homepage,
      Annotations.countByAnnotatedThing(thing.id),
      Places.countPlacesForThing(thing.id)))
  
      
  /** Writes an annotation **/
  implicit val annotationWrites: Writes[Annotation] = (
    (JsPath \ "uuid").write[String] ~
    (JsPath \ "in_dataset").write[String] ~
    (JsPath \ "annotated_item").write[String] ~
    (JsPath \ "place").write[GazetteerURI]
  )(a => (
      a.uuid.toString,
      a.dataset,
      a.annotatedThing,
      a.gazetteerURI))
      
  implicit val indexedObjectWrites: Writes[IndexedObject] = (
    (JsPath \ "id").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "description").writeNullable[String]
  )(o => (
      o.id,
      o.title,
      o.description))
      
      
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