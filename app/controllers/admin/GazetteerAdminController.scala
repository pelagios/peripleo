package controllers.admin

import models.geo.{ Gazetteers, Gazetteer }
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.Logger
import global.Global
import play.api.libs.json.Json
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import java.sql.Date
import models.ImportStatus
import ingest.harvest.GazetteerImporter

object GazetteerAdminController extends BaseUploadController with Secured {
  
  def index = adminAction { username => implicit requestWithSession =>
    Ok(views.html.admin.gazetteers(Gazetteers.listAll().map(_._1)))
  }
  
  def deleteGazetteer(name: String) = adminAction { username => implicit requestWithSession =>
    val gazetteer = Gazetteers.findByName(name)
    if (gazetteer.isDefined) {
      Logger.info("Deleting gazetteer: " + name)
      
      Gazetteers.delete(gazetteer.get.name)
      
      Global.index.deleteGazetter(gazetteer.get.name)
      Logger.info("Done.")
      Status(200)
    } else {
      NotFound
    }
  }
  
  def uploadGazetteerDump = adminAction { username => implicit requestWithSession =>    
    val json = requestWithSession.request.body.asJson
    if (json.isDefined) {
      val url = (json.get \ "url").as[String]
      Logger.info("Importing from " + url + " not implemented yet")

      // TODO implement!
      
      Ok(Json.parse("{ \"message\": \"Not implemented yet.\" }"))   
    } else {
      processUpload("rdf", requestWithSession, { filepart => {
        val file = filepart.ref.file      // The file
        val filename = filepart.filename
        val gazetteerName = filename.substring(0, filename.indexOf("."))

        Logger.info("Importing gazetteer '" + gazetteerName + "' from " + filename)
        
        val importer = new GazetteerImporter(Global.index)
        importer.importDataFile(file.getAbsolutePath, gazetteerName, Some(filename))
        Redirect(routes.GazetteerAdminController.index).flashing("success" -> { "Import in progress." })      
      }})
    }
  }
  
  def queryStatus(gazetteerName: String) = adminAction { username => implicit requestWithSession =>
    val progress = 0 // GazetteerImporter.getProgress(gazetteerName)
    Logger.info("Progress: " + (progress * 100) + "%")
    Ok(Json.parse("{ \"progress\": " + progress + " }"))
  }

}