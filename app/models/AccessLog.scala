package models

import java.sql.Timestamp
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }

/** LogRecord model entity **/
case class LogRecord(id: Option[Int], timestamp: Timestamp, path: String, referrer: String, ip: String)

/** Tag DB table **/
class AccessLog(slickTag: SlickTag) extends Table[LogRecord](slickTag, "access_log") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def timestamp = column[Timestamp]("timestamp", O.NotNull)
  
  def path = column[String]("path", O.NotNull)
  
  def referrer = column[String]("referrer", O.NotNull)

  def ip = column[String]("ip", O.NotNull)
  
  def * = (id.?, timestamp, path, referrer, ip) <> (LogRecord.tupled, LogRecord.unapply)
	
}

/** Queries **/
object AccessLog {
  
  private[models] val query = TableQuery[AccessLog]
  
  /** Creates the DB table **/
  def create()(implicit s: Session) = query.ddl.create

}

