package models

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

private[models] case class PlacesByDataset(id: Option[Int], dataset: String, gazetteerURI: GazetteerURI, count: Int)

private[models] class PlacesByDatasetTable(tag: Tag) extends Table[PlacesByDataset](tag, "places_by_dataset") with HasGazetteerURIColumn {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    
  def datasetId = column[String]("dataset", O.NotNull)

  def gazetteerURI = column[GazetteerURI]("gazetteer_uri", O.NotNull)

  def count = column[Int]("count", O.NotNull)
  
  def * = (id.?, datasetId, gazetteerURI, count) <> (PlacesByDataset.tupled, PlacesByDataset.unapply)
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  /** Indices **/
    
  def annotatedThingIdx = index("idx_datasets_by_place", datasetId, unique = false)

  def gazetterUriIdx = index("idx_places_by_dataset", gazetteerURI, unique = false)
  
}

private[models] case class PlacesByThing(id: Option[Int], dataset: String, annotatedThing: String, gazetteerURI: GazetteerURI, count: Int)

private[models] class PlacesByThingTable(tag: Tag) extends Table[PlacesByThing](tag, "places_by_annotated_thing") with HasGazetteerURIColumn {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    
  def datasetId = column[String]("dataset", O.NotNull)
  
  def annotatedThingId = column[String]("annotated_thing", O.NotNull)

  def gazetteerURI = column[GazetteerURI]("gazetteer_uri", O.NotNull)

  def count = column[Int]("count", O.NotNull)
  
  def * = (id.?, datasetId, annotatedThingId, gazetteerURI, count) <> (PlacesByThing.tupled, PlacesByThing.unapply)
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  def annotatedThingFk = foreignKey("annotated_thing_fk", annotatedThingId, AnnotatedThings.query)(_.id)
  
  /** Indices **/
    
  def datasetIdx = index("idx_datasets_by_place_and_thing", datasetId, unique = false)
  
  def annotatedThingIdx = index("idx_things_by_place_and_dataset", annotatedThingId, unique = false)
  
  def gazetterUriIdx = index("idx_places_by_dataset_and_thing", gazetteerURI, unique = false)
  
}

object Places extends HasGazetteerURIColumn {
  
  private val queryByDataset = TableQuery[PlacesByDatasetTable]
  
  private val queryByThing = TableQuery[PlacesByThingTable]
  
  def create()(implicit s: Session) = {
    queryByDataset.ddl.create
    queryByThing.ddl.create
  }
  
  private[models] def purge(datasetId: String)(implicit s: Session) = {
    queryByDataset.where(_.datasetId === datasetId).delete
    queryByThing.where(_.datasetId === datasetId).delete
  }
  
  private[models] def recompute(datasetId: String)(implicit s: Session) = {
    purge(datasetId)
      
    // Load all annotations for this dataset from the DB
    val annotations = Annotations.findByDataset(datasetId).items
      
    // Compute per-dataset stats and insert
    val placesInDataset = annotations.groupBy(_.gazetteerURI).mapValues(_.size).toSeq
      .map { case (gazetteerUri, count) => PlacesByDataset(None, datasetId, gazetteerUri, count) }
    queryByDataset.insertAll(placesInDataset:_*)
      
    // Compute per-thing stats and insert
    val placesByAnnotatedThing = annotations.groupBy(_.annotatedThing).toSeq.flatMap { case (thingId, annotations) => {
      annotations.groupBy(_.gazetteerURI).mapValues(_.size).toSeq
        .map { case (gazetteerUri, count) => PlacesByThing(None, datasetId, thingId, gazetteerUri, count)}
    }}
    queryByThing.insertAll(placesByAnnotatedThing:_*)
  }
  
  def countDatasetsForPlace(gazetteerURI: GazetteerURI)(implicit s: Session): Int =
    Query(queryByDataset.where(_.gazetteerURI === gazetteerURI).length).first
  
  def findDatasetsForPlace(gazetteerURI: GazetteerURI)(implicit s: Session): Seq[(Dataset, Int)] = {
    val query = for {
      placesByDataset <- queryByDataset.where(_.gazetteerURI === gazetteerURI)   
      dataset <- Datasets.query if placesByDataset.datasetId === dataset.id
    } yield (dataset, placesByDataset.count)
    
    query.list
  }
  
  def countThingsForPlaceAndDataset(gazetteerURI: GazetteerURI, datasetId: String)(implicit s: Session): Int =
    Query(queryByThing.where(_.datasetId === datasetId).where(_.gazetteerURI === gazetteerURI).length).first
  
  def findThingsForPlaceAndDataset(gazetteerURI: GazetteerURI, datasetId: String)(implicit s: Session): Seq[(AnnotatedThing, Int)] = {
    val query = for {
      placesByThing <- queryByThing.where(_.datasetId === datasetId).where(_.gazetteerURI === gazetteerURI)   
      annotatedThing <- AnnotatedThings.query if placesByThing.annotatedThingId === annotatedThing.id
    } yield (annotatedThing, placesByThing.count)
    
    query.list    
  }
  
  def countPlacesInDataset(datasetId: String)(implicit s: Session): Int =
    Query(queryByDataset.where(_.datasetId === datasetId).length).first
 
  def findPlacesInDataset(datasetId: String, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[(GazetteerURI, Int)] = {
    val total = countPlacesInDataset(datasetId)
    val result = queryByDataset.where(_.datasetId === datasetId).drop(offset).take(limit)
      .map(row => (row.gazetteerURI, row.count)).list
    
    Page(result, offset, limit, total)
  }
  
  def countPlacesForThing(thingId: String)(implicit s: Session): Int =
    Query(queryByThing.where(_.annotatedThingId === thingId).length).first
    
  def findPlacesForThing(thingId: String, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[(GazetteerURI, Int)] = {
    val total = countPlacesForThing(thingId)
    val result = queryByThing.where(_.annotatedThingId === thingId).drop(offset).take(limit)
      .map(row => (row.gazetteerURI, row.count)).list
      
    Page(result, offset, limit, total)
  }
  
}