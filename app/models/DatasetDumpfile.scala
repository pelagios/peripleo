package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag, Query }
import java.sql.Timestamp
import play.api.Logger

/** Dataset model entity **/
case class DatasetDumpfile(

  uri: String,
  
  datasetId: String,
  
  lastHarvest: Option[Timestamp]

)

class DatasetDumpfiles(tag: Tag) extends Table[DatasetDumpfile](tag, "dataset_dumpfiles") {
	
  def uri = column[String]("uri", O.PrimaryKey)

  def datasetId = column[String]("datatset", O.NotNull)
  
  def lastHarvest = column[Timestamp]("last_harvest", O.Nullable)
  
  def * = (uri, datasetId, lastHarvest.?) <> (DatasetDumpfile.tupled, DatasetDumpfile.unapply)

  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)

}

object DatasetDumpfiles {

  private[models] val query = TableQuery[DatasetDumpfiles]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(dumpfile: DatasetDumpfile)(implicit s: Session) = query.insert(dumpfile)
  
  def insertAll(dumpfiles: Seq[DatasetDumpfile])(implicit s: Session) = query.insertAll(dumpfiles:_*)

}
