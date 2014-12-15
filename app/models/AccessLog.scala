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

  def uuid = column[UUID]("uuid", O.PrimaryKey)
  
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
  
  def listAll()(implicit s: Session): Seq[AccessLogRecord] =
    query.list
  
  def findAllBefore(timestamp: Long)(implicit s: Session): Seq[AccessLogRecord] =
    query.where(_.timestamp < new Timestamp(timestamp)).list
    
  def deleteAllBefore(timestamp: Long)(implicit s: Session) =
    query.where(_.timestamp < new Timestamp(timestamp)).delete
       
}

class AccessLogAnalytics(log: Seq[AccessLogRecord]) {
  
  /** Number of hits to page URLs **/
  lazy val pageHits: Int = log.count(record => record.path.contains("/pages"))
  
  /** Number of hits to API URLs **/
  lazy val apiHits: Int = log.size - pageHits
  
  private lazy val searchRecords = log.filter(_.path.contains("/search"))
  
  private def getSearchTerm(path: String): Option[String] = {
    val startIdx = path.indexOf("query=")
    if (startIdx > -1) {
      val endIdx = path.indexOf('&', startIdx + 6)
      if (endIdx > -1)
        Some(path.substring(startIdx + 6, endIdx))
      else
        Some(path.substring(startIdx + 6))
    } else {
      None
    }
  }
  
  /** Most frequent search terms on the API **/
  lazy val searches: Seq[(String, Int)] =
    searchRecords
      .flatMap(record => getSearchTerm(record.path))
      .groupBy(term => term)
      .map(t => (t._1, t._2.size))
      .toSeq
      .sortBy(- _._2)
  
}

