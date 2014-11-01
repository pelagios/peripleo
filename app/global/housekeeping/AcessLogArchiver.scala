package global.housekeeping

import java.io.{ BufferedInputStream, File, FileInputStream, FileOutputStream, PrintWriter }
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{ Calendar, Date }
import java.util.zip.{ ZipEntry, ZipOutputStream }
import java.util.concurrent.TimeUnit
import models.{ AccessLog, LogRecord }
import play.api.db.slick._
import play.api.Play
import play.api.Play.current
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.Files.TemporaryFile
import scala.concurrent.duration
import scala.concurrent.duration.Duration

object AccessLogArchiver {
  
  private val SEPARATOR = ";"
    
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss")
    
  private lazy val LOG_ARCHIVE_DIR = {
    val dir = new File(Play.current.configuration.getString("api.log.archival.folder").getOrElse("logs"))
    if (!dir.exists)
      dir.mkdirs()
    dir
  }
  
  /** Shorthand **/
  private def setTime(c: Calendar, hour: Int, minute: Int) = {
    c.set(Calendar.HOUR_OF_DAY, hour)
    c.set(Calendar.MINUTE, minute)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
  }

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
    setTime(c, startTime(0).toInt, startTime(1).toInt)
    
    // Are we already past that time this month? Set next month.
    if (c.getTimeInMillis < now.getTime)
      c.add(Calendar.MONTH, 1)
      
    Logger.info("Scheduled next log archival run for " + new Date(c.getTimeInMillis()))
      
    // Time until start time
    Duration(c.getTimeInMillis - now.getTime, TimeUnit.MILLISECONDS)
  }
  
  private def writeToZip(date: Calendar, records: Seq[LogRecord]) = {
    // Helper to add leading 0 to single-digit numbers
    def format(i: Int) = if (i < 10) "0" + i else i.toString
      
    val filename =
      "accesslogs-" +
      date.get(Calendar.YEAR) + "-" + 
      format(date.get(Calendar.MONTH) + 1) + "-" +
      format(date.get(Calendar.DAY_OF_MONTH))
      
    // Write log records to CSV
    val csvFile = new TemporaryFile(new File(LOG_ARCHIVE_DIR, filename + ".csv"))
    val csvFileWriter = new PrintWriter(csvFile.file)

    val header = 
      Seq("timestamp", "unix time", "ip", "path", "response time", "user agent", "referrer", "accept header")
    csvFileWriter.write(header.mkString(SEPARATOR) + "\n")
      
    records.foreach(r => {
      val line =
        Seq(dateFormat.format(r.timestamp),
            r.timestamp.getTime.toString,
            r.ip,
            r.path,
            r.responseTime.toString,
            r.userAgent,
            r.referrer,
            r.accept)
          
      csvFileWriter.write(line.mkString(SEPARATOR) + "\n")  
    })

    csvFileWriter.flush()
    csvFileWriter.close()
    
    // Zip the CSV
    val zipFile = new File(LOG_ARCHIVE_DIR, filename + ".zip")
    val zipStream = new ZipOutputStream(new FileOutputStream(zipFile, false))
    
    zipStream.putNextEntry(new ZipEntry(filename + ".csv"))
    val in = new BufferedInputStream(new FileInputStream(csvFile.file))
    var b = in.read()
    while (b > -1) {
      zipStream.write(b)
      b = in.read()
    }
    in.close()
    zipStream.closeEntry()
    
    zipStream.flush()
    zipStream.close()
    
    // Remove the original CSV
    csvFile.finalize()
  }
  
  def start() = {    
    Akka.system.scheduler.scheduleOnce(delay) {
      
      DB.withSession { implicit s: Session =>
        val startTime = System.currentTimeMillis
        
        // Retrieve all log records older than one month
        val dateLimit = Calendar.getInstance()
        dateLimit.setTime(new Date())
        setTime(dateLimit, 0, 0) // Last night, 0am
        dateLimit.add(Calendar.MONTH, -1) // One month before
        
        val dateLimitMillis = dateLimit.getTimeInMillis
        Logger.info("Archiving access logs older than " + new Date(dateLimitMillis))

        val toArchive = AccessLog.findAllBefore(dateLimitMillis)
        if (toArchive.size > 0) {
          Logger.info(toArchive.size + " log records")
          
          // Write to file
          writeToZip(dateLimit, toArchive)
        
          // Drop from database
          AccessLog.deleteAllBefore(dateLimitMillis)
          Logger.info("Archiving complete - took " + (System.currentTimeMillis - startTime) + " ms")
        } else {
          Logger.info("Nothing to archive - skipping")
        }
      }
    }
  }

}