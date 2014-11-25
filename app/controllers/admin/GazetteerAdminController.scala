package controllers.admin

import models.geo.{ Gazetteers, Gazetteer }
import play.api.db.slick._
import play.api.mvc.Controller
import play.api.Logger
import global.Global
import play.api.libs.json.Json
import java.util.zip.GZIPInputStream
import java.io.FileInputStream

object GazetteerAdminController extends BaseUploadController with Secured {
  
  def index = adminAction { username => implicit requestWithSession =>
    Ok(views.html.admin.gazetteers(Gazetteers.listAll.map(_._1)))
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
        val filename = filepart.filename  // The full filename, e.g. dump.ttl.gz
        
        Logger.info("Importing gazetteer dump from file: " + filepart.filename)
        val (is, filenameUncompressed)  = if (filename.endsWith(".gz"))
          (new GZIPInputStream(new FileInputStream(file)), filename.substring(0, filename.lastIndexOf('.')))
        else
          (new FileInputStream(file), filename)
        
        val gazetteerName = filenameUncompressed.substring(0, filenameUncompressed.lastIndexOf("."))
        val (totalPlaces, _, prefixes) = Global.index.addPlaceStream(is, filenameUncompressed, gazetteerName)
        Global.index.refresh()
        Gazetteers.insert(Gazetteer(gazetteerName, totalPlaces), prefixes)
        
        Redirect(routes.GazetteerAdminController.index).flashing("success" -> { "Gazetteer imported." })      
      }})
    }
  }

}