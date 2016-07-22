package controllers.admin

import java.io.File
import global.Global
import ingest.harvest.GazetteerImporter
import models.geo.{ Gazetteers, Gazetteer }
import play.api.db.slick._
import play.api.mvc.{ BodyParsers, Controller }
import play.api.Logger
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

object GazetteerAdminController extends BaseUploadController with Secured {

  def index = adminAction { username => implicit requestWithSession =>
    Ok(views.html.admin.gazetteers())
  }

  def deleteGazetteer(name: String) = DBAction { implicit requestWithSession =>
    val gazetteer = Gazetteers.findByName(name)
    if (gazetteer.isDefined) {
      Logger.info("Deleting gazetteer: " + name)

      Gazetteers.delete(gazetteer.get.name)

      Global.index.deleteGazetter(gazetteer.get.name.toLowerCase)
      Logger.info("Done.")
      Status(200)
    } else {
      Status(404)
    }
  }

  def uploadGazetteerDump = DBAction(BodyParsers.parse.anyContent) { implicit requestWithSession =>
    val json = requestWithSession.request.body.asJson
    if (json.isDefined) {
      val url = (json.get \ "url").as[String]
      Logger.info("Importing from " + url + " not implemented yet")

      // TODO implement!

      Ok(Json.parse("{ \"message\": \"Not implemented yet.\" }"))
    } else {
      processUpload("rdf", requestWithSession, { filepart => {
    		try {
    			// Original name of the uploaded file
    		    val filename = filepart.filename
    		    val gazetteerName = filename.substring(0, filename.indexOf("."))
    
    		    // Play apparently removes the file after first read... But ingest will
    		    // need to read the file twice (once to count the places, second to import
    		    // them) so we create a copy here
    		    val tempFile = filepart.ref.file
    		    val copy = new File(tempFile.getAbsolutePath + "_cp")
    		    Files.copyFile(tempFile, copy, true, true)
    
    		    val importer = new GazetteerImporter(Global.index)
    		    val future = importer.importDataFileAsync(copy.getAbsolutePath, gazetteerName, Some(filename))
    		    future.onComplete(_ => {
    		      Logger.info("Deleting file " + copy.getAbsolutePath)
    		      copy.delete()
    		    })

            // Redirect(routes.GazetteerAdminController.index).flashing("success" -> { "Import in progress." })
		        Status(201)
    		} catch {
    		    case e: Exception =>  {
    		  	  Logger.error("exception caught: " + e)
    		  	  Status(500)
    		  	}
    		}
      }})
    }
  }

}
