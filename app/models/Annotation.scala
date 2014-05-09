package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import java.util.UUID

case class Annotation(uuid: UUID, dataset: String, target: String, gazetterURI: String)

class Annotations(tag: Tag) extends Table[Annotation](tag, "annotations") {

  def uuid = column[UUID]("uuid", O.PrimaryKey)
  
  def datasetId = column[String]("dataset", O.NotNull)
  
  def targetId = column[String]("target", O.NotNull)
  
  def gazetteerURI = column[String]("gazetteer_uri", O.NotNull)
  
  def * = (uuid, datasetId, targetId, gazetteerURI) <> (Annotation.tupled, Annotation.unapply)
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  def targetFk = foreignKey("target_fk", targetId, AnnotatedThings.query)(_.id)
  
  /** Indices **/
  
  def datasetIdx = index("dataset_idx", datasetId, unique = false)
  
}

object Annotations {
  
  private[models] val query = TableQuery[Annotations]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(annotations: Seq[Annotation])(implicit s: Session) = query.insertAll(annotations:_*)
  
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
 
}
