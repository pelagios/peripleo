package ingest.harvest

import index.Index
import java.io.{ File, FileInputStream }
import java.sql.Date
import java.util.zip.GZIPInputStream
import models.ImportStatus
import models.geo.{ Gazetteer, Gazetteers }
import global.Global
import play.api.Logger
import play.api.db.slick._
import play.api.Play.current
import org.pelagios.Scalagios
import index.places.ImportFuture
import java.io.InputStream
import play.api.libs.Files

class GazetteerImportWorker(index: Index) {
  
  var totalPlacesToImport: Long = 0
  
  var placesImportedInPreviousBatches: Long = 0
  
  var placeStreamImporter: Option[ImportFuture] = None
  
  var placesProcessed: Long = 0
  
  private def getFilenameAndInputStream(file: File, origFilename: Option[String] = None): (String, InputStream) = {
    val filename = 
      if (origFilename.isDefined)
        origFilename.get
      else
        file.getName
        
    if (filename.endsWith(".gz"))
      (filename, new GZIPInputStream(new FileInputStream(file)))
    else
      (filename, new FileInputStream(file))
  }
  
  /** Helper fn to insert a single dump file into the index.
    * 
    * Returns number of total places, distinct places and URI prefixes
    */
  private def insertDumpfile(file: File, gazetteerName: String, origFilename: Option[String] = None): Unit = {
    val (filename, is) = getFilenameAndInputStream(file, origFilename)
    val importer = index.addPlaceStreamAsync(is, filename, gazetteerName)
    importer.success((total, distinct, prefixes) => {
      // TODO update DB
    })

    importer.error(message => {
      // TODO update DB
    })
    
    importer.progress(processed => {
      
      placesProcessed = processed
      
    })

    importer.run() // Blocking execution
  }
  
  /** Helper function to count number of places in a file, so we can estimate progress while we're streaming **/
  private def countPlaces(file: File, origFilename: Option[String] = None): Long = {
    val (filename, is) = getFilenameAndInputStream(file, origFilename) 
    val count = Scalagios.countPlaces(is, filename)
    is.close()
    count
  }
  
  def importDataDump(dataDumpPath: String, gazetteerName: String, origFilename: Option[String] = None) = {
    val tempfile = new File(dataDumpPath)
    
    Files.copyFile(tempfile, new File("/home/simonr/foo"), true, true)
    val file = new File("/home/simonr/foo")
    
    // val (totalPlaces, distinctPlaces, uriPrefixes) =
      if (file.isDirectory) {
        // For each file, count RDF place resources 
        totalPlacesToImport = file.listFiles.map(f => countPlaces(f, origFilename)).sum        
        // file.listFiles.foldLeft((0, 0, Seq.empty[String])) { case ((totalPlaces, distinctPlaces, uriPrefixes), nextFile) => {
        file.listFiles.foreach(file => {
          Logger.info("Loading partial gazetteer file: " + file.getName)
          // val (newPlaces, newDistinctPlaces, prefixes) = insertDumpfile(nextFile, gazetteerName, origFilename)
          // Logger.info("Inserted " + (totalPlaces + newPlaces) + " places")
          // (totalPlaces + newPlaces, distinctPlaces + newDistinctPlaces, (uriPrefixes ++ prefixes).distinct)
          insertDumpfile(file, gazetteerName, origFilename)
        })
      } else {
        val keepAlive = new FileInputStream(file)

        Logger.info("Counting places...")
        Logger.info(file.getParentFile.list().mkString(", "))
        totalPlacesToImport = countPlaces(file, origFilename)
        Logger.info(totalPlacesToImport + " places in dumpfile")
        
        Logger.info(file.getParentFile.list().mkString(", "))
        insertDumpfile(new File(file.getAbsolutePath), gazetteerName, origFilename)
        
        keepAlive.close()
      }

    index.refresh()
      
    /* Insert gazetteer meta in to DB
    DB.withSession { implicit session: Session =>
      val now = new Date(new java.util.Date().getTime)
      Gazetteers.insert(Gazetteer(gazetteerName, totalPlaces, now, ImportStatus.COMPLETE), uriPrefixes)
    }
    */
    
  }
  
  def getProgress(): Double = {
    placeStreamImporter match {
      case Some(importer) => {
        Logger.info("yo")
        placesProcessed // (placesImportedInPreviousBatches + importer.placesProcessedCount).toDouble / totalPlacesToImport
      }
      case _ => { Logger.info("Ingest not yet running"); 0.0 }
    }
  }
  
}