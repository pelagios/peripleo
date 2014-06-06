package models

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import global.Global
import com.vividsolutions.jts.geom.Geometry
import play.api.Logger

/** Helper entity to speed up 'how many places are in dataset XY'-type queries.
  *
  * In a nutshell, this table contains links between datasets and places, plus a bit
  * of 'cached' information about the place from the gazetteer, so that we don't
  * need to do an extra gazetteer resolution step when retrieving the links from 
  * the DB. 
  */
private[models] case class PlacesByDataset(
    
  /** Auto-inc ID **/
  id: Option[Int], 
    
  /** ID of the dataset that is referencing the place **/
  dataset: String, 
    
  /** Cached information about the place (URI, title and geometry) **/
  place: GazetteerReference, 
    
  /** Number of times the place is referenced **/
  count: Int)

    
private[models] class PlacesByDatasetTable(tag: Tag) extends Table[PlacesByDataset](tag, "places_by_dataset") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    
  def datasetId = column[String]("dataset", O.NotNull)

  def gazetteerURI = column[String]("gazetteer_uri", O.NotNull)
  
  def title = column[String]("title", O.NotNull)
  
  def geometry = column[GeoJSON]("geometry", O.Nullable)

  def count = column[Int]("count", O.NotNull)
  
  // Solution for embedding GazetteerURI as multiple columns provided by the mighty @manuelbernhardt
  def * = (id.?, datasetId, (gazetteerURI, title, geometry.?), count).shaped <> (
    { case (id, datasetId, gazetteerURI, count) => PlacesByDataset(id, datasetId, GazetteerReference.tupled.apply(gazetteerURI), count) },
    { p: PlacesByDataset => Some(p.id, p.dataset, GazetteerReference.unapply(p.place).get, p.count) })
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  /** Indices **/
    
  def annotatedThingIdx = index("idx_datasets_by_place", datasetId, unique = false)

  def gazetterUriIdx = index("idx_places_by_dataset", gazetteerURI, unique = false)
  
}

/** Helper entity to speed up 'how many places are in annotated item XY'-type queries **/
private[models] case class PlacesByThing(
    
  /** Auto-inc ID **/
  id: Option[Int], 
  
  /** ID of the dataset containing the annotating thing **/
  dataset: String, 
  
  /** ID of the annotated thing that is referencing the place **/
  annotatedThing: String, 
  
  /** Cached information about the place (URI, title and geometry) **/
  place: GazetteerReference,
  
  /** Number of times the place is referenced **/
  count: Int)

private[models] class PlacesByThingTable(tag: Tag) extends Table[PlacesByThing](tag, "places_by_annotated_thing") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    
  def datasetId = column[String]("dataset", O.NotNull)
  
  def annotatedThingId = column[String]("annotated_thing", O.NotNull)

  def gazetteerURI = column[String]("gazetteer_uri", O.NotNull)
  
  def title = column[String]("title", O.NotNull)
  
  def geometry = column[GeoJSON]("geometry", O.Nullable)

  def count = column[Int]("count", O.NotNull)
  
  // Solution for embedding GazetteerURI as multiple columns provided by the mighty @manuelbernhardt
  def * = (id.?, datasetId, annotatedThingId, (gazetteerURI, title, geometry.?), count).shaped <> (
    { case (id, datasetId, annotatedThingId, gazetteerURI, count) => PlacesByThing(id, datasetId, annotatedThingId, GazetteerReference.tupled.apply(gazetteerURI), count) },
    { p: PlacesByThing => Some(p.id, p.dataset, p.annotatedThing, GazetteerReference.unapply(p.place).get, p.count) })
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  def annotatedThingFk = foreignKey("annotated_thing_fk", annotatedThingId, AnnotatedThings.query)(_.id)
  
  /** Indices **/
    
  def datasetIdx = index("idx_datasets_by_place_and_thing", datasetId, unique = false)
  
  def annotatedThingIdx = index("idx_things_by_place_and_dataset", annotatedThingId, unique = false)
  
  def gazetterUriIdx = index("idx_places_by_dataset_and_thing", gazetteerURI, unique = false)
  
}

/** Queries **/
object Places {
  
  private val queryByDataset = TableQuery[PlacesByDatasetTable]
  
  private val queryByThing = TableQuery[PlacesByThingTable]
  
  def create()(implicit s: Session) = {
    queryByDataset.ddl.create
    queryByThing.ddl.create
  }

  private def recomputeDataset(datasetId: String)(implicit s: Session) = {
    // Purge
    queryByDataset.where(_.datasetId === datasetId).delete
    
    // Recompute
    val placesInDataset = Annotations.findByDataset(datasetId).items.groupBy(_.gazetteerURI)
      .map { case (uri, annotations) => (Global.gazetteer.findByURI(uri), annotations.size) }
      .filter(_._1.isDefined) // We restrict to places in the gazetteer
      .map { case (place, count) => 
        PlacesByDataset(None, datasetId, GazetteerReference(place.get.uri, place.get.title, place.get.locations.headOption.map(l => GeoJSON(l.geoJSON))), count) }
      .toSeq

    queryByDataset.insertAll(placesInDataset:_*)    
  }
  
  private def recomputeLeafThings(datasetId: String, annotations: Seq[Annotation])(implicit s: Session) = {    
    val annotationsByThing = annotations.groupBy(_.annotatedThing)
    annotationsByThing.foreach { case (thingId, annotations) => 
      queryByThing.where(_.annotatedThingId === thingId).delete }
    
    val placesByThing = annotationsByThing.flatMap { case (thingId, annotations) => {
      annotations.groupBy(_.gazetteerURI)
        .map { case (uri, annotations) => (Global.gazetteer.findByURI(uri), annotations.size) }
        .filter(_._1.isDefined) // We restrict to places in the gazetteer
        .map { case (place, count) => 
          PlacesByThing(None, datasetId, thingId, GazetteerReference(place.get.uri, place.get.title, place.get.locations.headOption.map(l => GeoJSON(l.geoJSON))), count) }
    }}.toSeq
    
    queryByThing.insertAll(placesByThing:_*)    
  }
  
  private def recomputeIntermediateThing(datasetId: String, intermediateThingId: String, leafThings: Seq[String], annotations: Seq[Annotation])(implicit s: Session) = {
    queryByThing.where(_.annotatedThingId === intermediateThingId).delete
    
    val annotationsForThing = annotations.filter(a => leafThings.contains(a.annotatedThing))
    val placesForThing = annotationsForThing.groupBy(_.gazetteerURI)
      .map { case (uri, annotations) => (Global.gazetteer.findByURI(uri), annotations.size) }
      .filter(_._1.isDefined) // We restrict to places in the gazetteer
      .map { case (place, count) =>
        PlacesByThing(None, datasetId, intermediateThingId, GazetteerReference(place.get.uri, place.get.title, place.get.locations.headOption.map(l => GeoJSON(l.geoJSON))), count) }
      .toSeq
      
    queryByThing.insertAll(placesForThing:_*)  
  }
  
  private[models] def recompute(annotations: Seq[Annotation])(implicit s: Session) = {
    Logger.info("Recomputing unique place count aggregates")
    
    // Recompute datasets and leaf things
    val affectedDatasets = annotations.groupBy(_.dataset).keys
    affectedDatasets.foreach(recomputeDataset(_))
    affectedDatasets.foreach(recomputeLeafThings(_, annotations))
      
    // Things can be hierarchical - aggregate starting from root nodes
    val leafThings = annotations.groupBy(_.annotatedThing).toSeq.map { case (thing, annotations) => (annotations.head.dataset, thing) }    
    Logger.info(leafThings.size + " annotated things")
    val hierarchies = leafThings.map { case (datasetId, thingId) => (datasetId, thingId, AnnotatedThings.getParentHierarchy(thingId)) }
    
    // Helper method that will get the leaf things for any intermediate thing in the hierarchy
    def getLeafThings(intermediateThing: String): Seq[String] = {
      hierarchies.filter(_._3.contains(intermediateThing)).map(_._2)
    }
    
    // Compute all intermediate things (as (datasetId, thingId) tuples)...
    val intermediateThings = hierarchies.flatMap { case (datasetId, thingId, parentIds) => (Seq.fill(parentIds.size)(datasetId)).zip(parentIds) }.distinct
    Logger.info(intermediateThings.size + " parent things in the hierarchy")

    // ...and recompute their stats
    intermediateThings.foreach { case (datasetId, intermediateThingId) => 
      recomputeIntermediateThing(datasetId, intermediateThingId, getLeafThings(intermediateThingId), annotations)}
  }
  
  def countDatasetsForPlace(gazetteerURI: String)(implicit s: Session): Int =
    Query(queryByDataset.where(_.gazetteerURI === gazetteerURI).length).first
  
  def findDatasetsForPlace(gazetteerURI: String)(implicit s: Session): Seq[(Dataset, Int)] = {
    val query = for {
      placesByDataset <- queryByDataset.where(_.gazetteerURI === gazetteerURI)   
      dataset <- Datasets.query if placesByDataset.datasetId === dataset.id
    } yield (dataset, placesByDataset.count)
    
    query.list
  }
  
  def countThingsForPlaceAndDataset(gazetteerURI: String, datasetId: String)(implicit s: Session): Int =
    Query(queryByThing.where(_.datasetId === datasetId).where(_.gazetteerURI === gazetteerURI).length).first
  
  def findThingsForPlaceAndDataset(gazetteerURI: String, datasetId: String)(implicit s: Session): Seq[(AnnotatedThing, Int)] = {
    val query = for {
      placesByThing <- queryByThing.where(_.datasetId === datasetId).where(_.gazetteerURI === gazetteerURI)   
      annotatedThing <- AnnotatedThings.query if placesByThing.annotatedThingId === annotatedThing.id
    } yield (annotatedThing, placesByThing.count)
    
    query.list    
  }
  
  def countPlacesInDataset(datasetId: String)(implicit s: Session): Int =
    Query(queryByDataset.where(_.datasetId === datasetId).length).first
 
  def findPlacesInDataset(datasetId: String, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[(GazetteerReference, Int)] = {
    val total = countPlacesInDataset(datasetId)
    val result = queryByDataset.where(_.datasetId === datasetId).sortBy(_.count.desc).drop(offset).take(limit).list
      .map(row => (row.place, row.count))
    
    Page(result, offset, limit, total)
  }
  
  def countPlacesForThing(thingId: String)(implicit s: Session): Int =
    Query(queryByThing.where(_.annotatedThingId === thingId).length).first
    
  def findPlacesForThing(thingId: String, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[(GazetteerReference, Int)] = {
    val total = countPlacesForThing(thingId)
    val result = queryByThing.where(_.annotatedThingId === thingId).sortBy(_.count.desc).drop(offset).take(limit).list
      .map(row => (row.place, row.count))
      
    Page(result, offset, limit, total)
  }
  
}