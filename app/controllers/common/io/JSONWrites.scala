package controllers.common.io

import global.Global
import index.IndexedObject
import index.places._
import models._
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
  implicit val placeWrites: Writes[IndexedPlace] = (
    (JsPath \ "gazetteer_uri").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "place_category").writeNullable[String] ~
    (JsPath \ "names").write[Seq[String]] ~
    (JsPath \ "description").writeNullable[String] ~
    (JsPath \ "location").writeNullable[JsValue] ~
    (JsPath \ "centroid_lat").writeNullable[Double] ~
    (JsPath \ "centroid_lng").writeNullable[Double]
  )(place => {
      (place.uri,
       place.title,
       place.category.map(_.toString),
       place.names.map(_.chars),
       place.description,
       place.geometryJson,
       place.centroid.map(_.y),
       place.centroid.map(_.x)) })  
  
  /** Writes a search result item **/
  implicit val indexedObjectWrites: Writes[IndexedObject] = (
    (JsPath \ "identifier").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "description").writeNullable[String] ~
    (JsPath \ "object_type").write[String] ~
    (JsPath \ "temporal_bounds_start").writeNullable[Int] ~
    (JsPath \ "temporal_bounds_end").writeNullable[Int]
  )(obj => (
      obj.identifier,
      obj.title,
      obj.description,
      obj.objectType.toString,
      obj.temporalBoundsStart,
      obj.temporalBoundsEnd))    
  
  /** Writes a network node **/
  implicit val networkNodeWrites: Writes[NetworkNode] = (
    (JsPath \ "uri").write[String] ~
    (JsPath \ "title").writeNullable[String] ~
    (JsPath \ "source_gazetteer").writeNullable[String]
  )(node => (
      node.uri,
      node.place.map(_.title),
      node.place.map(_.sourceGazetteer)))
      
  /** Writes a network edge **/
  implicit val networkEdgeWrites: Writes[NetworkEdge] = (
    (JsPath \ "source").write[Int] ~
    (JsPath \ "target").write[Int]
  )(edge => (
      edge.source,
      edge.target)) 
      
  implicit val networkWrites: Writes[IndexedPlaceNetwork] = (
    (JsPath \ "nodes").write[Seq[NetworkNode]] ~
    (JsPath \ "edges").write[Seq[NetworkEdge]]
  )(network => (
      network.nodes,
      network.edges))
      
  /** Writes a Gazetteer URI, with place data pulled from the index on the fly **/
  implicit def gazetteerURIWrites(implicit verbose: Boolean = true): Writes[GazetteerReference] = (
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

     
  /** Writes a pair (Place, Occurrence-Count) **/
  implicit def placeCountWrites(implicit verbose: Boolean = true): Writes[(GazetteerReference, Int)] = (
      (JsPath).write[GazetteerReference] ~
      (JsPath \ "num_occurrences").write[Int]
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
       Places.countPlacesInDataset(dataset.id),
       subsetsJson)})


  /** Writes dataset dumpfile metadata **/  
  implicit val datasetDumpfileWrites: Writes[DatasetDumpfile] = (
    (JsPath \ "uri").write[String] ~
    (JsPath \ "dataset").write[String] ~
    (JsPath \ "last_harvest").writeNullable[Long]
  )(dumpfile => (
      dumpfile.uri,
      dumpfile.datasetId,
      dumpfile.lastHarvest.map(_.getTime)))
      
      
  /** Writes a pair (Dataset, Seq[DatasetDumpfile]) **/
  implicit def datasetWithDumpsWrites(implicit s: Session): Writes[(Dataset, Seq[DatasetDumpfile])] = (
    (JsPath).write[Dataset] ~
    (JsPath \ "dumpfiles").write[Seq[DatasetDumpfile]]
  )(t  => (t._1, t._2))   

        
  /** Writes an annotated thing, with annotation count and place count pulled from the DB on the fly **/ 
  implicit def annotatedThingWrites(implicit s: Session): Writes[AnnotatedThing] = (
    (JsPath \ "id").write[String] ~
    (JsPath \ "title").write[String] ~
    (JsPath \ "in_dataset").write[String] ~
    (JsPath \ "is_part_of").writeNullable[String] ~
    (JsPath \ "homepage").writeNullable[String] ~
    (JsPath \ "num_subitems").writeNullable[Int] ~
    (JsPath \ "num_annotations").write[Int] ~ 
    (JsPath \ "num_unique_places").write[Int]
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
