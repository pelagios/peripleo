package ingest.harvest

import global.Global
import index.Index
import java.io.{ File, FileInputStream }
import java.sql.Timestamp
import java.util.zip.GZIPInputStream
import models.ImportStatus
import models.geo.{ Gazetteer, Gazetteers }
import org.pelagios.Scalagios
import org.pelagios.api.gazetteer.Place
import play.api.Logger
import play.api.db.slick._
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import scala.collection.mutable.Set
import scala.concurrent.Future

class GazetteerImporter(index: Index) {
  
  private val MIN_STATUS_UPDATE_WAIT_TIME = 3000
  
  /** Helper to get the appropriate - GZIP or uncompressed - inputstream for the file **/
  private def getFilenameAndInputStream(file: File, origFilename: Option[String] = None) = {
    val filename = if (origFilename.isDefined) origFilename.get else file.getName
    if (filename.endsWith(".gz"))
      (filename, new GZIPInputStream(new FileInputStream(file)))
    else
      (filename, new FileInputStream(file))
  }
  
  /** Creates the initial gazetteer record in the DB **/
  private def createGazetteerRecordInDB(gazetteerName: String) =
    DB.withSession { implicit session: Session =>
      val now = new Timestamp(System.currentTimeMillis)
      Gazetteers.insert(Gazetteer(gazetteerName, 0, now, ImportStatus.PENDING, None, None))
    }
  
  /** Updates the gazetteer import info in the DB **/
  private def updateImportStatus(gazetteerName: String,status: ImportStatus.Value,
      progress: Option[Double], message: Option[String], totalPlaces: Option[Long]) =
        
    DB.withSession { implicit session: Session =>
      Gazetteers.setImportStatus(gazetteerName, status, progress, message, totalPlaces)
    }
  
  /** Updates the URI prefixes in the databaes **/
  private def setURIPrefixes(gazetteerName: String, prefixes: Seq[String]) = {
    DB.withSession { implicit session: Session =>
      Gazetteers.setURIPrefixes(gazetteerName, prefixes)
    }
  }
  
  /** Counts the places contained in the specified RDF dump file **/
  private def countPlacesInDump(file: File, origFilename: Option[String]): Long = {
    val (filename, is) = getFilenameAndInputStream(file, origFilename) 
    val count = Scalagios.countPlaces(is, filename)
    is.close()
    count
  }
  
  /** The ingest loop **/
  private def ingestDump(file: File, gazetteerName: String, origFilename: Option[String], totalPlacesInDump: Long) = {
    val (filename, is) = getFilenameAndInputStream(file, origFilename)
    
    // Shorthand for no. of places that equal approx. 1% of total places in dump
    val onePercent = Math.round(totalPlacesInDump.toDouble / 100)  
    
    // Mutable set for collecting prefixes
    val uriPrefixes = Set.empty[String] 
    
    // Counters for total places ingested and distinct new places created
    var placesIngested = 0
    var distinctNewPlaces = 0
    
    // Time when last status update was written to DB
    var lastStatusUpdateTime = 0l
    
    def placeHandler(place: Place): Unit = {
      // Add place
      val isDistinct = index.addPlace(place, gazetteerName, uriPrefixes)
      
      // Increment counters
      placesIngested += 1
      if (isDistinct)
        distinctNewPlaces += 1
        
      // Check if we should write a status update
      if ((placesIngested % onePercent == 0) && 
          (System.currentTimeMillis - lastStatusUpdateTime) > MIN_STATUS_UPDATE_WAIT_TIME) {
          
        val progress = placesIngested.toDouble / totalPlacesInDump
        updateImportStatus(gazetteerName, ImportStatus.IMPORTING, Some(progress), None, Some(placesIngested))
        lastStatusUpdateTime = System.currentTimeMillis
      }
    }
    
    val format = Scalagios.guessFormatFromFilename(filename)
    if (format.isDefined) {
      Scalagios.readPlacesFromStream(is, format.get, placeHandler, true)
      (placesIngested, distinctNewPlaces, uriPrefixes.toSeq)
    } else {
      throw new RuntimeException("Could not determine format for file " + filename)
    }    
  }
  
  /** Imports a gazetteer data file into the index.
    *
    * Progress info is persisted to the database in regular intervals, at steps of approx.
    * 1%, but at a maximum of 1 update every 3 seconds.   
    */
  def importDataFile(path: String, gazetteerName: String, origFilename: Option[String] = None) = {
    Logger.info("Importing gazetteer from file: " + path) 
    try {
      val file = new File(path)
            
      // Step 1: create the gazetteer record in the DB
      createGazetteerRecordInDB(gazetteerName)
      
      // Step 2: count places in dump
      val totalPlacesInDump = countPlacesInDump(file, origFilename)
      Logger.info("File contains " + totalPlacesInDump + " place records")
      
      // Step 3: import - update status in DB at regular intervals
      val (placesIngested, distinctNewPlaces, uriPrefixes) =
        ingestDump(file, gazetteerName, origFilename, totalPlacesInDump)
        
      // Step 4: refresh index readers
      index.refresh()
      
      // Step 5: update gazetteer record in DB
      updateImportStatus(gazetteerName, ImportStatus.COMPLETE, Some(1.0), None, Some(placesIngested))
      setURIPrefixes(gazetteerName, uriPrefixes)
    } catch {
      case t: Throwable => {
        updateImportStatus(gazetteerName, ImportStatus.FAILED, None, Some(t.getMessage()), None)
        Logger.info("Gazetteer import error: " + t.getMessage)
        t.printStackTrace()
      }
    }
      
  }
  
  def importDataFileAsync(path: String, gazetteerName: String, origFilename: Option[String]): Future[Unit] = Future {
    importDataFile(path, gazetteerName, origFilename)
  } 
  
}