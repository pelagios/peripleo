package global

import index.Index
import java.util.zip.GZIPInputStream
import java.io.{ File, FileInputStream }
import models._
import org.openrdf.rio.RDFFormat
import org.pelagios.Scalagios
import play.api.Play
import play.api.Play.current
import play.api.{ Application, GlobalSettings, Logger }
import play.api.db.slick._
import scala.slick.jdbc.meta.MTable

object Global extends GlobalSettings {
  
  private val GAZETTER_DATA_DIR = "gazetteer"
       
  /** Initializes the object index **/
  lazy val index = {
    val idx = Index.open("index")
    
    if (idx.numPlaces == 0) {
      Logger.info("Building new place index")
      
      def getPropertyAsList(name: String): Seq[String] = 
        Play.current.configuration.getString(name).map(_.split(",").toSeq).getOrElse(Seq.empty[String]).map(_.trim)
      
      val gazetteers = getPropertyAsList("api.gazetteer.names").zip(getPropertyAsList("api.gazetteer.files"))
      
      DB.withSession { implicit session: Session =>
        gazetteers.foreach { case (name, dump) => {
          Logger.info("Loading gazetteer " + name  + ": " + dump)
        
          val is = if (dump.endsWith(".gz"))
              new GZIPInputStream(new FileInputStream(new File(GAZETTER_DATA_DIR, dump)))
            else
              new FileInputStream(new File(GAZETTER_DATA_DIR, dump))
        
          val places = Scalagios.readPlaces(is, "http://pelagios.org/", RDFFormat.TURTLE).toSeq
          val uriPrefixes = places.map(_.uri).map(uri => uri.substring(0, uri.indexOf('/', 8))).distinct
          uriPrefixes.foreach(prefix => Logger.info("URI prefix '" + prefix + "'"))
          
          val names = places.flatMap(_.names)
          Logger.info("Inserting " + places.size + " places with " + names.size + " names into index")
          val distinctPlaces = idx.addPlaces(places)
          idx.refresh()
          
          // Insert gazetteer meta in to DB
          Gazetteers.insert(Gazetteer(name, places.size, distinctPlaces), uriPrefixes)
        }}
      }
    }

    idx
  }

  override def onStart(app: Application): Unit = {
    // Initializes the database schema
    DB.withSession { implicit session: Session =>
      if (MTable.getTables("annotated_things").list().isEmpty) {
        Logger.info("DB table 'annotated_things' does not exist - creating")
        AnnotatedThings.create
      }
       
      if (MTable.getTables("annotations").list().isEmpty) {
        Logger.info("DB table 'annotations' does not exist - creating")
        Annotations.create
      }
      
      if (MTable.getTables("datasets").list().isEmpty) {
        Logger.info("DB table datasets does not exist - creating")
        Datasets.create
      }
      
      if (MTable.getTables("places_by_dataset").list().isEmpty && MTable.getTables("places_by_annotated_thing").list().isEmpty) {
        Logger.info("Places index tables do not exist - creating")
        Places.create
      }
      
      if (MTable.getTables("gazetteers").list().isEmpty) {
        Logger.info("DB table gazetteers does not exist - creating")
        Gazetteers.create
      }
    }
  }  
  
  override def onStop(app: Application): Unit = index.close()

}