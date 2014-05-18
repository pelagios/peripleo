package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

case class AnnotatedThing(id: String, dataset: String, title: String, isPartOf: Option[String])

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

object AnnotatedThings {
  
  private[models] val query = TableQuery[AnnotatedThings]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(thing: AnnotatedThing)(implicit s: Session) = 
    query.insert(thing)
  
  def update(thing: AnnotatedThing)(implicit s: Session) = 
    query.where(_.id === thing.id).update(thing)
  
  def listAll(offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[AnnotatedThing] = {
    val total = countAll()
    val result = query.drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }
  
  def countAll()(implicit s: Session): Int = 
    Query(query.length).first
  
  def findById(id: String)(implicit s: Session): Option[AnnotatedThing] = 
    query.where(_.id === id).firstOption
    
  def findByDataset(id: String, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[AnnotatedThing] = {
    val total = countByDataset(id)
    val result = query.where(_.datasetId === id).drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }
  
  def countByDataset(id: String)(implicit s: Session): Int =
    Query(query.where(_.datasetId === id).length).first
 
}
