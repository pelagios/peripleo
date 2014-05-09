package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import java.sql.Date

/** Dataset case class.
  *  
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class Dataset(
    
    /** Internal ID in the API **/
    id: String,
    
    /** dcterms:title **/
    title: String, 
    
    /** dcterms:publisher **/
    publisher: String, 
    
    /** dcterms:license **/
    license: String, 
    
    /** time the dataset was created in the system **/
    created: Date,
    
    /** URL of the VoID file (unless imported via file upload) **/
    voidURI: Option[String], 
    
    /** dcterms:description **/
    description: Option[String],
    
    /** foaf:homepage **/
    homepage: Option[String],
    
    /** void:dataDump **/
    datadump: Option[String],
    
    /** time the dataset was last modified in the system **/
    modified: Option[Date])

/** Dataset database table **/
class Datasets(tag: Tag) extends Table[Dataset](tag, "datasets") {

  def id = column[String]("id", O.PrimaryKey)
  
  def title = column[String]("title", O.NotNull)
  
  def publisher = column[String]("publisher", O.NotNull)
  
  def license = column[String]("license", O.NotNull)
  
  def created = column[Date]("created", O.NotNull)
  
  def voidURI = column[String]("void_uri", O.Nullable)
  
  def description = column[String]("description", O.Nullable)
  
  def homepage = column[String]("homepage", O.Nullable)
  
  def datadump = column[String]("datadump", O.Nullable)
  
  def modified = column[Date]("modified", O.Nullable)
  
  def * = (id, title, publisher, license, created, voidURI.?, description.?, 
    homepage.?, datadump.?, modified.?) <> (Dataset.tupled, Dataset.unapply)
  
}

object Datasets {
  
  private[models] val query = TableQuery[Datasets]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(dataset: Dataset)(implicit s: Session) = query.insert(dataset)
  
  def update(dataset: Dataset)(implicit s: Session) = query.where(_.id === dataset.id).update(dataset)
 
  def listAll(offset: Int = 0, limit: Int = 20)(implicit s: Session): Page[Dataset] = {
    val total = countAll()
    val result = query.drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }
  
  def countAll()(implicit s: Session): Int = Query(query.length).first
  
  def findById(id: String)(implicit s: Session): Option[Dataset] = 
    query.where(_.id === id).firstOption
    
  def delete(id: String)(implicit s: Session) =
    query.where(_.id === id).delete
 
}
