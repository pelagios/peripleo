package controllers.common.io

import models._
import java.io.FileInputStream
import org.pelagios.Scalagios
import org.openrdf.rio.RDFFormat
import org.openrdf.rio.UnsupportedRDFormatException
import play.api.db.slick._
import play.api.Logger
import play.api.libs.Files._
import play.api.mvc.MultipartFormData._
import play.api.mvc.RequestHeader
import java.sql.Date

object VoIDImporter extends BaseImporter {
  
  def importVoID(file: FilePart[TemporaryFile], uri: Option[String] = None)(implicit s: Session, r: RequestHeader) = {
    Logger.info("Importing VoID file: " + file.filename)

    val format = file.filename match {
      case f if f.endsWith("rdf") => RDFFormat.RDFXML
      case f if f.endsWith("ttl") => RDFFormat.TURTLE
      case f if f.endsWith("n3") => RDFFormat.N3
      case _ => throw new UnsupportedRDFormatException("Format not supported")
    }
    
    // If we don't have a base URI for the VoID file, we'll use our own namespace as fallback
    // Not 100% the Sesame parser actually makes use of it... but we're keeping things sane nonetheless
    val baseURI = uri.getOrElse(controllers.routes.DatasetController.listAll.absoluteURL(false)(r))
    Scalagios.readVoID(new FileInputStream(file.ref.file), baseURI, format).foreach(dataset => {
      val id =
        if (dataset.uri.startsWith("http://")) {
          md5(dataset.uri)          
        } else {
          md5(dataset.title + " " + dataset.publisher)
        }
      
      Logger.info("Importing dataset '" + dataset.title + "' with ID " + id)
      Datasets.insert(Dataset(id, dataset.title, dataset.publisher, dataset.license,
        new Date(System.currentTimeMillis), uri, dataset.description, dataset.homepage, 
        dataset.datadumps.headOption, None))
    })
  }
  
}