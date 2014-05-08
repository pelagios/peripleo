package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

case class Dataset(id: String, title: String, publisher: String)

class Datasets(tag: Tag) extends Table[Dataset](tag, "datasets") {

  def id = column[String]("id", O.PrimaryKey)
  
  def title = column[String]("title", O.NotNull)
  
  def publisher = column[String]("publisher", O.NotNull)
  
  def * = (id, title, publisher) <> (Dataset.tupled, Dataset.unapply)
  
}

object Datasets {
  
  private[models] val query = TableQuery[Datasets]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(dataset: Dataset)(implicit s: Session) = query.insert(dataset)
  
  def update(dataset: Dataset)(implicit s: Session) = query.where(_.id === dataset.id).update(dataset)
  
  def listAll()(implicit s: Session): Seq[Dataset] = query.list
  
  def countAll()(implicit s: Session): Int = Query(query.length).first
  
  def findById(id: String)(implicit s: Session): Option[Dataset] = 
    query.where(_.id === id).firstOption
 
}
