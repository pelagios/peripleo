package controllers.common.io

import global.Global
import index.IndexedObject
import index.places._
import models._
import play.api.db.slick._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/** JSON writers for model and index classes. **/
object JSONWrites {
  
  /**                                **/
  /** Database entity serializations **/
  /**                                **/
  
  /** TODO this inlines subitem/annotation/place counts with a DB request - optimize via a Writes[(AnnotatedThing, Seq[Depiction], Seq[Thumbnail], Int, Int, Int)] **/ 
  implicit def annotatedThingWrites(implicit s: Session): Writes[AnnotatedThing] = (
    (JsPath \ "identifier").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "in_dataset").write[String] ~
    (JsPath \ "is_part_of").writeNullable[String] ~
    (JsPath \ "homepage").writeNullable[String] ~
    (JsPath \ "description").writeNullable[String] ~
    (JsPath \ "temporal_bounds").writeNullable[JsValue] ~
    (JsPath \ "geo_bounds").writeNullable[BoundingBox] ~
    // TODO change image format
    (JsPath \ "thumbnails").writeNullable[Seq[String]] ~
    (JsPath \ "images").writeNullable[Seq[String]] ~
    (JsPath \ "num_subitems").writeNullable[Int] ~
    (JsPath \ "num_annotations").write[Int] ~ 
    (JsPath \ "num_unique_places").write[Int]
  )(thing => { 
     val (thumbnails, depictions) = {
       val (t, d) = Images.findByAnnotatedThing(thing.id)
       (if (t.size > 0) Some(t.map(_.url)) else None,
        if (d.size > 0) Some(d.map(_.url)) else None) 
     }
     
     (thing.id,
      thing.title,
      thing.dataset,
      thing.isPartOf,
      thing.homepage,
      thing.description,
      thing.temporalBoundsStart.map(start => Json.obj( 
        "start" -> start,
        "end" -> { val end = thing.temporalBoundsEnd.getOrElse(start); end })),
      thing.convexHull.map(_.bounds),
      thumbnails,
      depictions,
      { val count = AnnotatedThings.countChildren(thing.id); if (count > 0) Some(count) else None },
      Annotations.countByAnnotatedThing(thing.id),
      AggregatedView.countPlacesForThing(thing.id))})
  
      
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
      
      
  /** TODO this inlines thing/annotation/place counts and subset meta - optimize via Writes[(Dataset, Seq[Datasets], Int, Int, Int)] **/
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
    (JsPath \ "num_items").write[Int] ~
    (JsPath \ "num_annotations").write[Int]  ~
    (JsPath \ "num_unique_places").write[Int] ~
    (JsPath \ "subsets").writeNullable[Seq[JsValue]]
  )(dataset => {
      val subsets = Datasets.listSubsets(dataset.id)
      val subsetsJson = 
        if (subsets.size > 0)
          Some(subsets.map(subset => Json.obj("id" -> subset.id, "title" -> subset.title)))
        else
          None
      (dataset.id,
       dataset.title,
       dataset.publisher,
       dataset.description,
       dataset.license,
       dataset.homepage,
       dataset.created.getTime,
       dataset.modified.getTime,
       dataset.voidURI,
       AnnotatedThings.countByDataset(dataset.id),
       Annotations.countByDataset(dataset.id),
       AggregatedView.countPlacesInDataset(dataset.id),
       subsetsJson)})
      
       
  implicit val datasetDumpfileWrites: Writes[DatasetDumpfile] = (
    (JsPath \ "uri").write[String] ~
    (JsPath \ "dataset").write[String] ~
    (JsPath \ "last_harvest").writeNullable[Long]
  )(dumpfile => (
      dumpfile.uri,
      dumpfile.datasetId,
      dumpfile.lastHarvest.map(_.getTime)))
      
      
  implicit def datasetWithDumpsWrites(implicit s: Session): Writes[(Dataset, Seq[DatasetDumpfile])] = (
    (JsPath).write[Dataset] ~
    (JsPath \ "dumpfiles").write[Seq[DatasetDumpfile]]
  )(t  => (t._1, t._2))   
  
  
  implicit def placeOccurrenceInDatasetWrites(implicit s: Session): Writes[(Dataset, Int)] = (
    (JsPath \ "dataset").write[Dataset] ~
    (JsPath \ "num_referencing_items").write[Int]
  )(t => (t._1, t._2))
      
      
  /** TODO this (optionally) inlines a place with an index request - optimize with a Writes[(Gazetteer, IndexedPlace)] **/
  implicit def gazetteerReferenceWrites(implicit verbose: Boolean = true): Writes[GazetteerReference] = (
    (JsPath \ "gazetteer_uri").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "location").writeNullable[JsValue] ~
    (JsPath \ "centroid_lat").writeNullable[Double] ~
    (JsPath \ "centroid_lng").writeNullable[Double] ~ 
    (JsPath).writeNullable[IndexedPlace]
  )(gRef => (
      gRef.uri,
      gRef.title,
      gRef.geometryJson.map(Json.parse(_)),
      gRef.centroid.map(_.y),
      gRef.centroid.map(_.x),
      { if (verbose) Global.index.findPlaceByURI(gRef.uri) else None })) 

     
  implicit def gazetteerReferenceWithCountWrites(implicit verbose: Boolean = true): Writes[(GazetteerReference, Int)] = (
      (JsPath).write[GazetteerReference] ~
      (JsPath \ "num_occurrences").write[Int]
  )(t  => (t._1, t._2))     
      
  
  implicit val bboxWrites: Writes[BoundingBox] = (
    (JsPath \ "min_lon").write[Double] ~
    (JsPath \ "max_lon").write[Double] ~
    (JsPath \ "min_lat").write[Double] ~
    (JsPath \ "max_lat").write[Double]
  )(bbox => (bbox.minLon, bbox.maxLon, bbox.minLat, bbox.maxLat))
  
  
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
      
      
  implicit val profileWrites: Writes[TemporalProfile] = (
    (JsPath \ "bounds_start").write[Int] ~
    (JsPath \ "bounds_end").write[Int] ~
    (JsPath \ "max_value").write[Int] ~
    (JsPath \ "histogram").write[Map[String, Int]]
  )(profile => (
      profile.boundsStart,
      profile.boundsEnd,
      profile.maxValue,
      profile.histogram.map(t => (t._1.toString, t._2))))
      
  /**                             **/
  /** Index entity serializations **/
  /**                             **/
      
  implicit val indexedObjectWrites: Writes[IndexedObject] = (
    (JsPath \ "identifier").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "description").writeNullable[String] ~
    (JsPath \ "homepage").writeNullable[String] ~
    (JsPath \ "object_type").write[String] ~
    (JsPath \ "temporal_bounds").writeNullable[JsValue] ~
    (JsPath \ "geo_bounds").writeNullable[BoundingBox]
  )(obj => (
      obj.identifier,
      obj.title,
      obj.description,
      obj.homepage,
      obj.objectType.toString,
      obj.temporalBoundsStart.map(start => Json.obj( 
        "start" -> start,
        "end" -> { val end = obj.temporalBoundsEnd.getOrElse(start); end })),
      obj.convexHull.map(_.bounds)))    
      
        
  implicit val placeWrites: Writes[IndexedPlace] = (
    (JsPath \ "gazetteer_uri").write[String] ~
    (JsPath \ "label").write[String] ~
    (JsPath \ "place_category").writeNullable[String] ~
    (JsPath \ "names").write[Seq[String]] ~
    (JsPath \ "description").writeNullable[String] ~
    (JsPath \ "location").writeNullable[JsValue] ~
    (JsPath \ "centroid_lat").writeNullable[Double] ~
    (JsPath \ "centroid_lng").writeNullable[Double]
  )(place => {
      (place.uri,
       place.label,
       place.category.map(_.toString),
       place.names.map(_.chars),
       place.description,
       place.geometryJson,
       place.centroid.map(_.y),
       place.centroid.map(_.x)) })  
  

  implicit def placeOccurencesWrites(implicit s: Session): Writes[(IndexedPlace, Seq[(Dataset, Int)])] = (
    (JsPath \ "to_place").write[IndexedPlace] ~
    (JsPath \ "occurrences").write[Seq[(Dataset, Int)]]
  )(t => (t._1, t._2))
       
  
  implicit val networkNodeWrites: Writes[NetworkNode] = (
    (JsPath \ "uri").write[String] ~
    (JsPath \ "label").writeNullable[String] ~
    (JsPath \ "source_gazetteer").writeNullable[String] ~
    (JsPath \ "is_inner_node").write[Boolean]
  )(node => (
      node.uri,
      node.place.map(_.label),
      node.place.map(_.sourceGazetteer),
      node.isInnerNode))
      

  implicit val networkEdgeWrites: Writes[NetworkEdge] = (
    (JsPath \ "source").write[Int] ~
    (JsPath \ "target").write[Int] ~
    (JsPath \ "is_inner_edge").write[Boolean]
  )(edge => (
      edge.source,
      edge.target,
      edge.isInnerEdge)) 
      
      
  implicit val networkWrites: Writes[IndexedPlaceNetwork] = (
    (JsPath \ "nodes").write[Seq[NetworkNode]] ~
    (JsPath \ "edges").write[Seq[NetworkEdge]]
  )(network => (
      network.nodes,
      network.edges))

}
