package ingest.harvest

import java.io.{ File, FileInputStream }
import java.sql.Date
import java.util.zip.GZIPInputStream
import models.ImportStatus
import models.geo.{ Gazetteer, Gazetteers }
import global.Global
import play.api.Logger
import play.api.db.slick._
import play.api.Play.current

class GazetteerImportWorker {
  
  /** Helper fn to insert a single dump file into the index.
    * 
    * Returns number of total places, distinct places and URI prefixes
    */
  private def insertDumpfile(file: File, gazetteerName: String): (Int, Int, Seq[String]) = {
    val (is, filename)  = 
      if (file.getName.endsWith(".gz"))
        (new GZIPInputStream(new FileInputStream(file)), file.getName.substring(0, file.getName.lastIndexOf('.')))
      else
        (new FileInputStream(file), file.getName)

    Global.index.addPlaceStream(is, filename, gazetteerName)
  }
  
  def importGazetteer(dataDumpPath: String, gazetteerName: String) = {
    Logger.info("Importing gazetteer " + gazetteerName  + " from " + dataDumpPath)

    val file = new File(dataDumpPath)
    val (totalPlaces, distinctPlaces, uriPrefixes) =
      if (file.isDirectory) {
        file.listFiles.foldLeft((0, 0, Seq.empty[String])) { case ((totalPlaces, distinctPlaces, uriPrefixes), nextFile) => {
          Logger.info("Loading partial gazetteer file: " + nextFile.getName)
          val (newPlaces, newDistinctPlaces, prefixes) = insertDumpfile(nextFile, gazetteerName)
          Logger.info("Inserted " + (totalPlaces + newPlaces) + " places")
          (totalPlaces + newPlaces, distinctPlaces + newDistinctPlaces, (uriPrefixes ++ prefixes).distinct)
        }}
      } else {
        insertDumpfile(file, gazetteerName)
      }

    Global.index.refresh()
      
    // Insert gazetteer meta in to DB
    DB.withSession { implicit session: Session =>
      val now = new Date(new java.util.Date().getTime)
      Gazetteers.insert(Gazetteer(gazetteerName, totalPlaces, now, ImportStatus.COMPLETE), uriPrefixes)
    }
    
  }
  
}