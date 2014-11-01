package models

import global.Global
import play.api.Logger
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }
import play.api.libs.json.Json
import index.places.IndexedPlace

private[models] case class PlaceToDataset(
    
    /** Auto-inc ID **/
    id: Option[Int],
    
    /** Dataset ID **/
    dataset: String, 
    
    /** Place reference **/
    place: GazetteerReference, 
    
    /** Weight: how often is the place referenced in the dataset **/
    count: Int)
    

/** A table holding associations between places and datasets. 
  *
  * These associations are a result of the connections between datasets, annotated things and 
  * annotations. (I.e. annotations relate places to annotated things; and the things are related
  * to datasets.)
  * 
  * In other words: this table introduces de-normalization for the sake of speeding up queries
  * on place-to-dataset associations. The model is essentially identical to the typical data-
  * warehouse star schema, with this table representing the fact table, and places and datasets
  * representing the dimensions.
  */ 
private[models] class PlaceToDatasetAssociations(tag: SlickTag) extends Table[PlaceToDataset](tag, "place_to_dataset_associations") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    
  def datasetId = column[String]("dataset", O.NotNull)

  def gazetteerURI = column[String]("gazetteer_uri", O.NotNull)
  
  def title = column[String]("title", O.NotNull)
  
  def location = column[String]("location", O.Nullable)

  def count = column[Int]("count", O.NotNull)
  
  // Solution for embedding GazetteerURI as multiple columns provided by the mighty @manuelbernhardt
  def * = (id.?, datasetId, (gazetteerURI, title, location.?), count).shaped <> (
    { case (id, datasetId, gazetteerURI, count) => PlaceToDataset(id, datasetId, GazetteerReference.tupled.apply(gazetteerURI), count) },
    { p: PlaceToDataset => Some(p.id, p.dataset, GazetteerReference.unapply(p.place).get, p.count) })
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  /** Indices **/
    
  def annotatedThingIdx = index("idx_datasets_by_place", datasetId, unique = false)

  def gazetterUriIdx = index("idx_places_by_dataset", gazetteerURI, unique = false)
  
}

private[models] case class PlaceToThing(
    
    /** Auto-inc ID */
    id: Option[Int], 
    
    /** Dataset ID **/
    dataset: String,
    
    /** AnnotatedThing ID **/
    annotatedThing: String, 
    
    /** Annotated Thing temporal bounds start **/
    temporalBoundsStart: Option[Int], 
    
    /** Annotated Thing temporal bounds end **/
    temporalBoundsEnd: Option[Int],

    /** Place reference **/
    place: GazetteerReference, 

    /** Weight: how often is the place referenced in the dataset **/
    count: Int)
    

/** A table holding associations between places and annotated things. 
  *
  * These associations are a result of the connections between annotated things and 
  * annotations (which related places to things).
  * 
  * In other words: this table introduces de-normalization for the sake of speeding up 
  * queries on place-to-thing associations. The model is essentially identical to the typical
  * data-warehouse star schema, with this table representing the fact table, and places and
  * annotated things representing the dimensions.
  */ 
private[models] class PlaceToThingAssociations(tag: SlickTag) extends Table[PlaceToThing](tag, "place_to_thing_associations") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    
  def datasetId = column[String]("dataset", O.NotNull)
  
  def annotatedThingId = column[String]("annotated_thing", O.NotNull)

  def temporalBoundsStart = column[Int]("temporal_bounds_start", O.Nullable)

  def temporalBoundsEnd = column[Int]("temporal_bounds_end", O.Nullable)

  def gazetteerURI = column[String]("gazetteer_uri", O.NotNull)
  
  def title = column[String]("title", O.NotNull)
  
  def location = column[String]("location", O.Nullable)

  def count = column[Int]("count", O.NotNull)
  
  // Solution for embedding GazetteerURI as multiple columns provided by the mighty @manuelbernhardt
  def * = (id.?, datasetId, annotatedThingId, temporalBoundsStart.?, temporalBoundsEnd.?, (gazetteerURI, title, location.?), count).shaped <> (
    { case (id, datasetId, annotatedThingId, temporalBoundsStart, temporalBoundsEnd, gazetteerURI, count) => PlaceToThing(id, datasetId, annotatedThingId, temporalBoundsStart, temporalBoundsEnd, GazetteerReference.tupled.apply(gazetteerURI), count) },
    { p: PlaceToThing => Some(p.id, p.dataset, p.annotatedThing, p.temporalBoundsStart, p.temporalBoundsEnd, GazetteerReference.unapply(p.place).get, p.count) })
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  def annotatedThingFk = foreignKey("annotated_thing_fk", annotatedThingId, AnnotatedThings.query)(_.id)
  
  /** Indices **/
    
  def datasetIdx = index("idx_datasets_by_place_and_thing", datasetId, unique = false)
  
  def annotatedThingIdx = index("idx_things_by_place_and_dataset", annotatedThingId, unique = false)
  
  def gazetterUriIdx = index("idx_places_by_dataset_and_thing", gazetteerURI, unique = false)
  
}

/** Queries **/
object Associations {
  
  private val queryByDataset = TableQuery[PlaceToDatasetAssociations]
  
  private val queryByThing = TableQuery[PlaceToThingAssociations]
  
  def create()(implicit s: Session) = {
    queryByDataset.ddl.create
    queryByThing.ddl.create
  }
  
  def insert(ingestBatch: Seq[(AnnotatedThing, Seq[(IndexedPlace, Int)])])(implicit s: Session) = {
    // Insert by-place records
    val placesByThing = ingestBatch.flatMap { case (thing, places) =>
      places.map { case (place, count) =>
        PlaceToThing(None, thing.dataset, thing.id, thing.temporalBoundsStart, thing.temporalBoundsEnd, 
          GazetteerReference(place.uri, place.label, place.geometryJson.map(Json.stringify(_))), count) }
    } 
    queryByThing.insertAll(placesByThing:_*)
    
    // Recompute by-dataset records
    val affectedDatasets = ingestBatch.map(_._1.dataset).distinct
    recomputeDatasets(affectedDatasets)
  }

  private def recomputeDatasets(leafIds: Seq[String])(implicit s: Session) = {
    // IDs of all affected datasets, including parents in the hierarchy
    val datasetIds = (leafIds ++ leafIds.flatMap(id => Datasets.getParentHierarchy(id))).distinct

    // Drop them from the table first...
    queryByDataset.where(_.datasetId.inSet(datasetIds)).delete
    
    // ...and then recompute
    datasetIds.foreach(id => {
      // Grab all annotations from the database and group by gazetteer URI
      val placesInDataset = Annotations.findByDataset(id).items.groupBy(_.gazetteerURI)
        .map { case (uri, annotations) => (Global.index.findPlaceByURI(uri), annotations.size) } // Resolve place from index and just keep annotation count
        .filter(_._1.isDefined) // We restrict to places in the gazetteer
        .map { case (place, count) => 
          PlaceToDataset(None, id, GazetteerReference(place.get.uri, place.get.label, place.get.geometryJson.map(Json.stringify(_))), count) }
        .toSeq
        
      // Write to DB
      queryByDataset.insertAll(placesInDataset:_*)
    })
  }
  
  def countDatasetsForPlace(gazetteerURI: String)(implicit s: Session): Int =
    Query(queryByDataset.where(_.gazetteerURI === gazetteerURI).length).first
  
  /** Returns the datasets that reference a specific place.
    * 
    * The results are tuples of
    * (i) dataset
    * (ii) no. of items in the set referencing the place
    * (iii) no. of occurrences (= annotations) in the set referencing the place 
    */
  def findOccurrences(gazetteerURI: String)(implicit s: Session): Seq[(Dataset, Int)] =
    findOccurrences(Set(gazetteerURI))
    
  def findOccurrences(gazetteerURIs: Set[String])(implicit s: Session): Seq[(Dataset, Int)] = {
    // Part 1: all things that reference the place
    val queryA = for { 
      (datasetId, thingId) <- queryByThing.where(_.gazetteerURI inSet gazetteerURIs).map(t => (t.datasetId, t.annotatedThingId))
      thing <- AnnotatedThings.query if thingId === thing.id
    } yield (datasetId, thing)
    
    // Part 2: filter to top-level things, group by dataset ID and join in the dataset
    val queryB = for {
      (datasetId, numberOfTopLevelThings) <- queryA.filter(_._2.isPartOfId.isNull).groupBy(_._1).map(t => (t._1, t._2.length))
      dataset <- Datasets.query if datasetId === dataset.id
    } yield (dataset, numberOfTopLevelThings)
    
    queryB.list
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
 
  def deleteForDatasets(ids: Seq[String])(implicit s: Session) = {
	 queryByThing.where(_.datasetId inSet ids).delete
	 queryByDataset.where(_.datasetId inSet ids).delete
  }
 
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
