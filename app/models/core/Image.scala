package models.core

import java.sql.Timestamp
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }

case class Image(
    
    /** ID **/
    id: Option[Int], 
    
    /** ID of the dataset **/
    dataset: String, 
    
    /** ID of the annotated thing **/
    annotatedThing: String, 
    
    /** Image URL **/
    url: String, 
    
    /** An image caption **/
    title: Option[String] = None,
    
    /** A (longer) description **/
    description: Option[String] = None,
    
    /** URL to a thumbnail image **/
    thumbnail: Option[String] = None,

    /** Image creator **/
    creator: Option[String] = None,
    
    /** Time of creation info **/
    created: Option[Timestamp] = None,

    /** Image license **/
    license: Option[String] = None)

    
class Images(tag: SlickTag) extends Table[Image](tag, "images") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def datasetId = column[String]("dataset", O.NotNull)
  
  def annotatedThingId = column[String]("annotated_thing", O.NotNull)
  
  def url = column[String]("url", O.NotNull)
  
  def title = column[String]("title", O.Nullable)
  
  def description = column[String]("description", O.Nullable, O.DBType("text"))
  
  def thumbnail = column[String]("thumbnail", O.Nullable)
  
  def creator = column[String]("creator", O.Nullable)
  
  def created = column[Timestamp]("created", O.Nullable)
  
  def license = column[String]("license", O.Nullable)
  
  def * = (id.?, datasetId, annotatedThingId, url, title.?, description.?, 
    thumbnail.?, creator.?, created.?, license.?) <> (Image.tupled, Image.unapply)

  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  def annotatedThingFk = foreignKey("annotated_thing_fk", annotatedThingId, AnnotatedThings.query)(_.id)
  
  /** Indices **/
  
  def annotatedThingIdx = index("idx_images_by_thing", annotatedThingId, unique = false)

}

/** Queries **/
object Images {
  
  private[models] val query = TableQuery[Images]
  
  /** Creates the DB table **/
  def create()(implicit s: Session) = query.ddl.create
  
  /** Inserts a list of Images into the DB **/
  def insertAll(images: Seq[Image])(implicit s: Session) =
    query.insertAll(images:_*)
    
  def deleteForDatasets(ids: Seq[String])(implicit s: Session) =
    query.where(_.datasetId inSet ids).delete
    
  def findByAnnotatedThing(id: String)(implicit s: Session): Seq[Image] =
    query.where(_.annotatedThingId === id).list
  
}