package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import global.Global

/** AnnotatedThing model entity **/
case class AnnotatedThing(id: String, dataset: String, title: String, isPartOf: Option[String])

/** AnnotatedThing DB table **/
class AnnotatedThings(tag: Tag) extends Table[AnnotatedThing](tag, "annotated_things") {

  def id = column[String]("id", O.PrimaryKey)
  
  def datasetId = column[String]("dataset", O.NotNull)
  
  def title = column[String]("title", O.NotNull)
  
  def isPartOfId = column[String]("is_part_of", O.Nullable)
  
  def * = (id, datasetId, title, isPartOfId.?) <> (AnnotatedThing.tupled, AnnotatedThing.unapply)
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  def isPartOf = foreignKey("is_part_of_fk", isPartOfId, AnnotatedThings.query)(_.id)
  
  /** Indices **/
  
  def datasetIdx = index("idx_things_by_dataset", datasetId, unique = false)
  
}

/** Queries **/
object AnnotatedThings {
  
  private[models] val query = TableQuery[AnnotatedThings]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(thing: AnnotatedThing)(implicit s: Session) = {
    query.insert(thing)
    Global.index.addAnnotatedThing(thing)
  }
  
  def update(thing: AnnotatedThing)(implicit s: Session) = 
    query.where(_.id === thing.id).update(thing)
  
  def countAll()(implicit s: Session): Int = 
    Query(query.length).first
    
  def listAll(offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[AnnotatedThing] = {
    val total = countAll()
    val result = query.drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }
  
  def findById(id: String)(implicit s: Session): Option[AnnotatedThing] = 
    query.where(_.id === id).firstOption

  def countByDataset(datasetId: String)(implicit s: Session): Int =
    Query(query.where(_.datasetId === datasetId).length).first

  def findByDataset(datasetId: String, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[AnnotatedThing] = {
    val total = countByDataset(datasetId)
    val result = query.where(_.datasetId === datasetId).drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }
  
}
