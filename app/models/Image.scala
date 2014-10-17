package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }

/** DB entity **/
case class Image(id: Option[Int], dataset: String, annotatedThing: String, url: String, isThumbnail: Boolean)

/** Wrappers to provide readable, typesafe access to depictions vs. thumbnails **/
case class Depiction(url: String)

case class Thumbnail(url: String)

/** DB table **/
class Images(tag: SlickTag) extends Table[Image](tag, "images") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def datasetId = column[String]("dataset", O.NotNull)
  
  def annotatedThingId = column[String]("annotated_thing", O.NotNull)
  
  def url = column[String]("url", O.NotNull)
  
  def isThumbnail = column[Boolean]("is_thumbnail", O.NotNull)
  
  def * = (id.?, datasetId, annotatedThingId, url, isThumbnail) <> (Image.tupled, Image.unapply)

  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  def annotatedThingFk = foreignKey("annotated_thing_fk", annotatedThingId, AnnotatedThings.query)(_.id)

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
    
  def findByAnnotatedThing(id: String)(implicit s: Session): (Seq[Thumbnail], Seq[Depiction]) = {
    val images = query.where(_.annotatedThingId === id).list
    val thumbnails = images.filter(_.isThumbnail).map(img => Thumbnail(img.url))
    val depictions = images.filter(!_.isThumbnail).map(img => Depiction(img.url))
    (thumbnails, depictions)
  }
  
}