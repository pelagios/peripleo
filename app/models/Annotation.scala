package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import java.util.UUID

case class Annotation(uuid: UUID, dataset: String, annotatedThing: String, gazetterURI: String)

class Annotations(tag: Tag) extends Table[Annotation](tag, "annotations") {

  def uuid = column[UUID]("uuid", O.PrimaryKey)
  
  def datasetId = column[String]("dataset", O.NotNull)
  
  def annotatedThingId = column[String]("annotated_thing", O.NotNull)
  
  def gazetteerURI = column[String]("gazetteer_uri", O.NotNull)
  
  def * = (uuid, datasetId, annotatedThingId, gazetteerURI) <> (Annotation.tupled, Annotation.unapply)
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  def annotatedThingFk = foreignKey("annotated_thing_fk", annotatedThingId, AnnotatedThings.query)(_.id)
  
  /** Indices **/
  
  def datasetIdx = index("idx_annotations_by_dataset", datasetId, unique = false)
  
  def annotatedThingIdx = index("idx_annotations_by_thing", annotatedThingId, unique = false)
  
}

object Annotations {
  
  private[models] val query = TableQuery[Annotations]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(annotations: Seq[Annotation])(implicit s: Session) = {
    // Insert annotations
    query.insertAll(annotations:_*)
    
    // Update place index stats for affected datasets
    annotations.groupBy(_.dataset).keys.foreach(Places.recompute(_))
  }
  
  def update(annotation: Annotation)(implicit s: Session) = 
    query.where(_.uuid === annotation.uuid).update(annotation)
  
  def listAll(offset: Int = 0, limit: Int = 20)(implicit s: Session): Page[Annotation] = {
    val total = countAll()
    val result = query.drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }
  
  def countAll()(implicit s: Session): Int = 
    Query(query.length).first
  
  def findByUUID(uuid: UUID)(implicit s: Session): Option[Annotation] = 
    query.where(_.uuid === uuid).firstOption
    
  def countByDataset(id: String)(implicit s: Session): Int =
    Query(query.where(_.datasetId === id).length).first
    
  def findByDataset(id: String, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[Annotation] = {
    val total = countByDataset(id)
    val result = query.where(_.datasetId === id).drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }
    
  def findByAnnotatedThing(id: String, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[Annotation] = {
    val total = countByAnnotatedThing(id)
    val result = query.where(_.annotatedThingId === id).drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }
  
  def countByAnnotatedThing(id: String)(implicit s: Session): Int =
    Query(query.where(_.annotatedThingId === id).length).first
 
}
