package global.housekeeping

import java.sql.Timestamp
import java.util.{ Calendar, Date }
import java.util.concurrent.TimeUnit
import play.api.db.slick._
import play.api.Play
import play.api.Play.current
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration
import scala.concurrent.duration.Duration

object AccessLogArchiver {

  /** Delay to initial archive run - defaults to 1st of month, 2am **/
  private val delay = {
    val startDay = Play.current.configuration.getString("api.log.archival.day").getOrElse("1").trim().toInt
    val startTime = Play.current.configuration.getString("api.log.archival.time").getOrElse("2:00").split(":")
    if (startTime.size != 2)
      throw new IllegalArgumentException("Configuration contains invalid log archival time setting")
    
    // Create a calendar with today month & day + time from app.conf
    val now = new Date();
    val c = Calendar.getInstance()
    c.setTime(now)
    c.set(Calendar.DAY_OF_MONTH, startDay)
    c.set(Calendar.HOUR_OF_DAY, startTime(0).toInt)
    c.set(Calendar.MINUTE, startTime(1).toInt)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    
    // Are we already past that time this month? Set next month.
    if (c.getTimeInMillis < now.getTime)
      c.add(Calendar.MONTH, 1)
      
    Logger.info("Scheduled next log archival run for " + new Date(c.getTimeInMillis()))
      
    // Time until start time
    Duration(c.getTimeInMillis - now.getTime, TimeUnit.MILLISECONDS)
  }

  def start() = {    
    Akka.system.scheduler.scheduleOnce(delay) {
      
      DB.withSession { implicit s: Session =>
        Logger.info("Archiving access logs...")
        
        /** TODO implement! **/
        
      }
    }
  }

}