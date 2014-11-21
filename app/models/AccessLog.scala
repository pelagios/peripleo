package models

import java.sql.Timestamp
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }
import java.util.UUID

/** LogRecord model entity **/
case class AccessLogRecord(uuid: UUID, timestamp: Timestamp, path: String, ip: String, userAgent: String, referrer: Option[String], accept: Option[String], responseTime: Int)

/** AccessLog DB table **/
class AccessLog(slickTag: SlickTag) extends Table[AccessLogRecord](slickTag, "access_log") {

  def uuid = column[UUID]("uuid", O.PrimaryKey, O.AutoInc)
  
  def timestamp = column[Timestamp]("timestamp", O.NotNull)
  
  def path = column[String]("path", O.NotNull)
  
  def ip = column[String]("ip", O.NotNull)
  
  def userAgent = column[String]("user_agent", O.NotNull)
  
  def referrer = column[String]("referrer", O.Nullable)
  
  def accept = column[String]("accept", O.Nullable)
  
  def responseTime = column[Int]("response_time", O.NotNull)

  def * = (uuid, timestamp, path, ip, userAgent, referrer.?, accept.?, responseTime) <> (AccessLogRecord.tupled, AccessLogRecord.unapply)
	
}

/** Queries **/
object AccessLog {
  
  private[models] val query = TableQuery[AccessLog]
  
  def create()(implicit s: Session) = query.ddl.create

  def insert(logRecord: AccessLogRecord)(implicit s: Session) = query.insert(logRecord)
  
  def findAllBefore(timestamp: Long)(implicit s: Session) =
    query.where(_.timestamp < new Timestamp(timestamp)).list
    
  def deleteAllBefore(timestamp: Long)(implicit s: Session) =
    query.where(_.timestamp < new Timestamp(timestamp)).delete
    
}

