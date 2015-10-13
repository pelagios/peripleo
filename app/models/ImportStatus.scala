package models

import play.api.db.slick.Config.driver.simple._

object ImportStatus extends Enumeration {
  
  val DOWNLOADING = Value("DOWNLOADING")
  
  val IMPORTING = Value("IMPORTING")
  
  val COMPLETE = Value("IMPORT_COMPLETE")
  
  val FAILED = Value("IMPORT_FAILED")
  
  implicit val statusMapper = MappedColumnType.base[ImportStatus.Value, String](
    { status => status.toString },
    { status => ImportStatus.withName(status) })
  
}

case class ImportProgress(status: ImportStatus.Value, totalProgress: Double, message: Option[String])
