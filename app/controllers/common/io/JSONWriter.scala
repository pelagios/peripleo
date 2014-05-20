package controllers.common.io

import models._
import play.api.db.slick._
import org.pelagios.api.gazetteer.Place
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import java.sql.Date
import global.Global

object JSONWriter {
  
  implicit val placeWrites: Writes[GazetteerURI] = (
    (JsPath \ "gazetteer_uri").write[String] ~
    (JsPath \ "title").writeNullable[String]
  )(uri => { 
      val place = Global.index.findByURI(uri.uri)
      (uri.uri, 
       place.map(_.title))})
 
  implicit val placeCountWrites: Writes[(GazetteerURI, Int)] = (
      (JsPath).write[GazetteerURI] ~
      (JsPath \ "count").write[Int]
  )(t  => (t._1, t._2))     
       
  implicit def datasetWrites(implicit s: Session): Writes[Dataset] = (
    (JsPath \ "id").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "publisher").write[String] ~
    (JsPath \ "description").writeNullable[String] ~
    (JsPath \ "license").write[String] ~
    (JsPath \ "homepage").writeNullable[String] ~
    (JsPath \ "created").write[Long] ~
    (JsPath \ "modified").writeNullable[Long] ~
    (JsPath \ "void_location").writeNullable[String] ~
    (JsPath \ "datadump_location").writeNullable[String] ~
    (JsPath \ "annotation_count").write[Int]  ~
    (JsPath \ "unique_place_count").write[Int]
  )(dataset => (
      dataset.id,
      dataset.title,
      dataset.publisher,
      dataset.description,
      dataset.license,
      dataset.homepage,
      dataset.created.getTime,
      dataset.modified.map(_.getTime),
      dataset.voidURI,
      dataset.datadump,
      Annotations.countByDataset(dataset.id),
      Places.countPlacesInDataset(dataset.id)))

      
      
  implicit def annotatedThingWrites(implicit s: Session): Writes[AnnotatedThing] = (
    (JsPath \ "id").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "in_dataset").write[String] ~
    (JsPath \ "is_part_of").writeNullable[String] ~
    (JsPath \ "annotation_count").write[Int] ~ 
    (JsPath \ "unique_place_count").write[Int]
  )(thing => (
      thing.id,
      thing.title,
      thing.dataset,
      thing.isPartOf,
      Annotations.countByAnnotatedThing(thing.id),
      Places.countPlacesForThing(thing.id)))
  
      
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
       
      
      
  implicit def pageWrites[A](implicit fmt: Writes[A]): Writes[Page[A]] = (
    (JsPath \ "total").write[Long] ~
    (JsPath \ "offset").write[Int] ~
    (JsPath \ "limit").write[Int] ~
    (JsPath \ "items").write[Seq[A]]
  )(page => (page.total, page.offset, page.limit, page.items))

}