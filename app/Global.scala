import models._
import play.api.Play
import play.api.Play.current
import play.api.{ Application, GlobalSettings, Logger }
import play.api.db.slick._
import scala.slick.jdbc.meta.MTable

/** Play Global object **/
object Global extends GlobalSettings {

  override def onStart(app: Application): Unit = {
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
        Logger.info("DB table datasets not exist - creating")
        Datasets.create
      }
    }
  }  

}