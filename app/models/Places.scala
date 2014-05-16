package models

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

case class PlacesByDataset(id: Option[Int], gazetteerURI: String, dataset: String, count: Int)

class PlacesByDatasetTable(tag: Tag) extends Table[PlacesByDataset](tag, "places_by_dataset") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gazetteerURI = column[String]("gazetteer_uri", O.NotNull)
  
  def datasetId = column[String]("dataset", O.NotNull)
  
  def count = column[Int]("count", O.NotNull)
  
  def * = (id.?, gazetteerURI, datasetId, count) <> (PlacesByDataset.tupled, PlacesByDataset.unapply)
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  /** Indices **/
  
  def gazetterUriIdx = index("idx_places_by_dataset", gazetteerURI, unique = false)
  
  def annotatedThingIdx = index("idx_datasets_by_place", datasetId, unique = false)
  
}

private[models] case class PlacesByThing(id: Option[Int], gazetteerURI: String, annotatedThing: String, count: Int)

class PlacesByThingTable(tag: Tag) extends Table[PlacesByThing](tag, "places_by_annotated_thing") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gazetteerURI = column[String]("gazetteer_uri", O.NotNull)
  
  def annotatedThingId = column[String]("annotated_thing", O.NotNull)
 
  def count = column[Int]("count", O.NotNull)
  
  def * = (id.?, gazetteerURI, annotatedThingId, count) <> (PlacesByThing.tupled, PlacesByThing.unapply)
  
  /** Foreign key constraints **/
  
  def annotatedThingFk = foreignKey("annotated_thing_fk", annotatedThingId, AnnotatedThings.query)(_.id)
  
  /** Indices **/
  
  def gazetterUriIdx = index("idx_places_by_thing", gazetteerURI, unique = false)
  
  def annotatedThingIdx = index("idx_things_by_place", annotatedThingId, unique = false)
  
}

object Places {
  
  private val queryByDataset = TableQuery[PlacesByDatasetTable]
  
  private val queryByThing = TableQuery[PlacesByThingTable]
  
  def create()(implicit s: Session) = {
    queryByDataset.ddl.create
    queryByThing.ddl.create
  }
  
  def findDatasetsByPlace(gazetteerURI: String): Seq[Dataset] =
    Seq.empty[Dataset]
  
  def findThingsByPlaceAndDataset(gazetteerURI: String, datasetId: String): Seq[AnnotatedThing]  =
    Seq.empty[AnnotatedThing]
  
  def findPlacesByDataset(id: String): Seq[String] =
    Seq.empty[String]
  
  def findPlacesByThing(id: String): Seq[String] = 
    Seq.empty[String]
  
}