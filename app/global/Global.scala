package global

import akka.actor.Cancellable
import global.housekeeping.AccessLogArchiver
import index.Index
import java.io.{ File, FileInputStream }
import java.util.zip.GZIPInputStream
import models._
import models.adjacency._
import models.core._
import models.geo._
import org.pelagios.api.gazetteer.Place
import org.pelagios.api.gazetteer.patch.{ PatchConfig, PatchStrategy }
import play.api.Play
import play.api.Play.current
import play.api.{ Application, GlobalSettings, Logger }
import play.api.db.slick._
import scala.slick.jdbc.meta.MTable
import java.sql.Date
import ingest.harvest.GazetteerImportWorker

object Global extends GlobalSettings {

  private val GAZETTER_DATA_DIR = "gazetteer"

  private var accessLogArchiver: Option[Cancellable] = None

  lazy val index = {
    val idx = Index.open("index")

    if (idx.numPlaceNetworks == 0) {
      Logger.info("Building place index...")

      // Grab the list of gazetteers from the application properties file
      def getPropertyAsList(name: String): Seq[String] =
        Play.current.configuration.getString(name).map(_.split(",").toSeq).getOrElse(Seq.empty[String]).map(_.trim)

      val gazetteers = getPropertyAsList("peripleo.gazetteer.names").zip(getPropertyAsList("peripleo.gazetteer.files"))
      Logger.info("Loading gazetteers: " + gazetteers.map(_.toString).mkString(", "))

      // Build the place index
      val worker = new GazetteerImportWorker(idx)
      gazetteers.foreach { case (name, dump) => {
        val path = new File(GAZETTER_DATA_DIR, dump).getAbsolutePath
        worker.importDataDump(path, name)
      }}

      // Apply gazetteer patches
      val patches = Play.current.configuration.getString("peripleo.gazetteer.patches")
        .map(_.split(",").toSeq).getOrElse(Seq.empty[String]).map(_.trim)

      // Patching strategy is to REPLACE geometries, but APPEND names
      val patchConfig = PatchConfig()
        .geometry(PatchStrategy.REPLACE)
        .names(PatchStrategy.APPEND)
        .propagate(true) // Patches should propagate to linke places, too

      patches.foreach(patch => {
        Logger.info("Applying gazetteer patch file " + patch)
        idx.applyPatch(new File(GAZETTER_DATA_DIR, patch), patchConfig)
        idx.refresh()
        Logger.info("Done.")
      })

      // Rebuild the suggestion index
      idx.suggester.build()
    }

    idx
  }

  override def onStart(app: Application): Unit = {
    // Initializes the database schema
    DB.withSession { implicit session: Session =>
      if (MTable.getTables("datasets").list.isEmpty) {
        Logger.info("DB table 'datasets' does not exist - creating")
        Datasets.create
      }

      if (MTable.getTables("annotated_things").list.isEmpty) {
        Logger.info("DB table 'annotated_things' does not exist - creating")
        AnnotatedThings.create
      }

      if (MTable.getTables("images").list.isEmpty) {
        Logger.info("DB table 'images' does not exist - creating")
        Images.create
      }

      if (MTable.getTables("annotations").list.isEmpty) {
        Logger.info("DB table 'annotations' does not exist - creating")
        Annotations.create
      }

      if (MTable.getTables("gazetteers").list.isEmpty) {
        Logger.info("DB table 'gazetteers' does not exist - creating")
        Gazetteers.create
      }

      if (MTable.getTables("place_to_dataset_associations").list.isEmpty || MTable.getTables("place_to_thing_associations").list.isEmpty) {
        Logger.info("DB tables 'place_to_dataset_associations' and 'place_to_thing_associations' do not exist - creating")
        Associations.create
      }

      if (MTable.getTables("tags").list.isEmpty) {
        Logger.info("DB table 'tags' does not exist - creating")
        Tags.create
      }

      if (MTable.getTables("adjacency_places").list.isEmpty) {
        Logger.info("DB table 'adjacency_place' does not exist - creating")
        PlaceAdjacencys.create
      }

      if (MTable.getTables("access_log").list.isEmpty) {
        Logger.info("DB table 'access_log' does not exit - creating")
        AccessLog.create
      }
    }

    // Start log archiving demon
    accessLogArchiver = Some(AccessLogArchiver.start())
  }

  override def onStop(app: Application): Unit = {
    index.close()

    if (accessLogArchiver.isDefined) {
      Logger.info("Shutting down log archival background actor")
      accessLogArchiver.get.cancel
      accessLogArchiver = None
    }
  }

}
