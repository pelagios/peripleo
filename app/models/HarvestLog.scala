package models

import java.util.UUID
import java.sql.Timestamp
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }

case class HarvestLogRecord(
  
  /** A globally unique ID for the harvest **/
  uuid: UUID, 
  
  /** Start time of the harvest **/
  startTime: Timestamp,
  
  /** VoID URL **/
  voidURL: String,
    
  /** MD5 hash of the VoID file **/
  voidHash: Option[String],
  
  /** MD5 hash of the annotation file(s) **/
  dataHash: Option[String],
  
  /** Time the harvest took **/
  harvestDuration: Int,
  
  /** Timne it tool to ingest the new data **/
  ingestDuration: Option[Int],

  /** Empty if everything went as planned, else contains an error message **/
  error: Option[String])

/** AccessLog DB table **/
class HarvestLog(slickTag: SlickTag) extends Table[HarvestLogRecord](slickTag, "harvest_log") {

  def uuid = column[UUID]("uuid", O.PrimaryKey, O.AutoInc)
  
  def startTime = column[Timestamp]("start_time", O.NotNull)
  
  def voidURL = column[String]("void_url", O.NotNull)
  
  def voidHash = column[String]("void_hash", O.Nullable)
  
  def dataHash = column[String]("data_hash", O.Nullable)
  
  def harvestDuration = column[Int]("harvest_duration", O.NotNull)
  
  def ingestDuration = column[Int]("ingest_duration", O.Nullable)
  
  def error = column[String]("error", O.Nullable)

  def * = (uuid, startTime, voidURL, voidHash.?, dataHash.?, harvestDuration, ingestDuration.?, error.?) <> (HarvestLogRecord.tupled, HarvestLogRecord.unapply)
	
}

/** Queries **/
object HarvestLog {
  
  private[models] val query = TableQuery[HarvestLog]
  
  def create()(implicit s: Session) = query.ddl.create

  def insert(logRecord: HarvestLogRecord)(implicit s: Session) = query.insert(logRecord)
  
}