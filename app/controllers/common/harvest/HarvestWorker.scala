package controllers.common.harvest

import java.net.URL
import java.io.File
import models.Datasets
import play.api.Play.current
import play.api.db.slick._
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import sys.process._
import controllers.common.io.{ CSVImporter, PelagiosOAImporter }
import scala.io.Source

class HarvestWorker {
  
  private val TMP_DIR = System.getProperty("java.io.tmpdir")
  private val UTF8 = "UTF-8"
  private val CSV = "csv"
	
  def harvest(datasetId: String) = {
    DB.withSession { implicit session: Session =>
      val d = Datasets.findById(datasetId)
      if (d.isEmpty) {
	    // TODO error notification
      } else {
	    val (dataset, dumpfileURLs) = d.get
 	    val dumpfiles = dumpfileURLs.par.map(d => { 
	      val filename = d.uri.substring(d.uri.lastIndexOf("/") + 1)
	      Logger.info("Downloading file " + filename + " from " + d.uri)
	      val tempFile = new TemporaryFile(new File(TMP_DIR, filename))
	      new URL(d.uri) #> tempFile.file !!
	      
	      Logger.info(filename + " - download complete.")
	      tempFile
	    }).seq
	    Logger.info("All downloads complete.")
	    
	    dumpfiles.foreach(file => {
	      Logger.info("Importing " + file.file.getName)
	      if (file.file.getName.endsWith(CSV))
            CSVImporter.importRecogitoCSV(Source.fromFile(file.file, UTF8), dataset)
          // TODO support OA imports - needs change in the importer interface!
          Logger.info(file.file.getName + " - import complete.")
	    })
	    Logger.info("All downloads imported.")
     }
    }
  }
	
}
