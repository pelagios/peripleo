package models

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

case class PlacesByDataset(id: Option[Int], dataset: String, gazetteerURI: String, count: Int)

class PlacesByDatasetTable(tag: Tag) extends Table[PlacesByDataset](tag, "places_by_dataset") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    
  def datasetId = column[String]("dataset", O.NotNull)

  def gazetteerURI = column[String]("gazetteer_uri", O.NotNull)

  def count = column[Int]("count", O.NotNull)
  
  def * = (id.?, datasetId, gazetteerURI, count) <> (PlacesByDataset.tupled, PlacesByDataset.unapply)
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  /** Indices **/
    
  def annotatedThingIdx = index("idx_datasets_by_place", datasetId, unique = false)

  def gazetterUriIdx = index("idx_places_by_dataset", gazetteerURI, unique = false)
  
}

private[models] case class PlacesByThing(id: Option[Int], dataset: String, annotatedThing: String, gazetteerURI: String, count: Int)

class PlacesByThingTable(tag: Tag) extends Table[PlacesByThing](tag, "places_by_annotated_thing") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    
  def datasetId = column[String]("dataset", O.NotNull)
  
  def annotatedThingId = column[String]("annotated_thing", O.NotNull)

  def gazetteerURI = column[String]("gazetteer_uri", O.NotNull)

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

object Places {
  
  private val queryByDataset = TableQuery[PlacesByDatasetTable]
  
  private val queryByThing = TableQuery[PlacesByThingTable]
  
  def create()(implicit s: Session) = {
    queryByDataset.ddl.create
    queryByThing.ddl.create
  }
  
  def purge(datasetId: String, annotatedThingId: Option[String] = None)(implicit s: Session) = {
    // TODO if only datasetId is specified, remove all references from both tables
    
    // TODO if datasetId and annotatedThingId is specified, remove all references to the
    // thing, and update the counts for the dataset
  }
  
  def update(datasetId: String, annotatedThingId: Option[String] = None)(implicit s: Session) = {
    // TODO if only datasetId is specified: (1) drop references if they exist, (2) recompute references
    // based on the annotation counts
    
    // TODO if both datasetId and thingId is specified: (1) drop references to the thing, (2) recompute
    // references to the thing, (3) recompute references for the dataset
  }
  
  def findDatasetsByPlace(gazetteerURI: String)(implicit s: Session): Seq[(Dataset, Int)] = {
    val query = for {
      placesByDataset <- queryByDataset.where(_.gazetteerURI === gazetteerURI)   
      dataset <- Datasets.query if placesByDataset.datasetId === dataset.id
    } yield (dataset, placesByDataset.count)
    
    query.list
  }
  
  def findThingsByPlaceAndDataset(gazetteerURI: String, datasetId: String)(implicit s: Session): Seq[(AnnotatedThing, Int)] = {
    val query = for {
      placesByThing <- queryByThing.where(_.gazetteerURI === gazetteerURI).where(_.datasetId === datasetId)   
      annotatedThing <- AnnotatedThings.query if placesByThing.annotatedThingId === annotatedThing.id
    } yield (annotatedThing, placesByThing.count)
    
    query.list    
  }
  
  def findPlacesByDataset(id: String)(implicit s: Session): Seq[(String, Int)] =
    queryByDataset.where(_.datasetId === id).map(row => (row.gazetteerURI, row.count)).list
  
  def findPlacesByThing(id: String)(implicit s: Session): Seq[(String, Int)] = 
    queryByThing.where(_.annotatedThingId === id).map(row => (row.gazetteerURI, row.count)).list
  
}