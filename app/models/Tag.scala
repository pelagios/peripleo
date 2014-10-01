package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }

/** Tag model entity **/
case class Tag(id: Option[Int], dataset: String, annotatedThing: String, label: String)

/** Tag DB table **/
class Tags(slickTag: SlickTag) extends Table[Tag](slickTag, "tags") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def datasetId = column[String]("dataset", O.NotNull)
  
  def annotatedThingId = column[String]("annotated_thing", O.NotNull)
  
  def label = column[String]("label", O.NotNull)
  
  def * = (id.?, datasetId, annotatedThingId, label) <> (Tag.tupled, Tag.unapply)

  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  def annotatedThingFk = foreignKey("annotated_thing_fk", annotatedThingId, AnnotatedThings.query)(_.id)
	
}

/** Queries **/
object Tags {
  
  private[models] val query = TableQuery[Tags]
  
  /** Creates the DB table **/
  def create()(implicit s: Session) = query.ddl.create

}

