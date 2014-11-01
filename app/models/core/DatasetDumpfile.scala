package models.core

import java.sql.Timestamp
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag, Query }

/** DatasetDumpfile model entity **/
case class DatasetDumpfile(

  uri: String,
  
  datasetId: String,
  
  lastHarvest: Option[Timestamp]

)

/** DatasetDumpfile DB table **/
class DatasetDumpfiles(tag: SlickTag) extends Table[DatasetDumpfile](tag, "dataset_dumpfiles") {
	
  def uri = column[String]("uri", O.PrimaryKey)

  def datasetId = column[String]("dataset", O.NotNull)
  
  def lastHarvest = column[Timestamp]("last_harvest", O.Nullable)
  
  def * = (uri, datasetId, lastHarvest.?) <> (DatasetDumpfile.tupled, DatasetDumpfile.unapply)

  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)

}

/** Queries **/
object DatasetDumpfiles {

  private[models] val query = TableQuery[DatasetDumpfiles]
  
  def create()(implicit s: Session) =
    query.ddl.create
  
  def insert(dumpfile: DatasetDumpfile)(implicit s: Session) = 
    query.insert(dumpfile)
  
  def insertAll(dumpfiles: Seq[DatasetDumpfile])(implicit s: Session) = 
    query.insertAll(dumpfiles:_*)
    
  def deleteForDatasets(ids: Seq[String])(implicit s: Session) = 
    query.where(_.datasetId inSet ids).delete

}
