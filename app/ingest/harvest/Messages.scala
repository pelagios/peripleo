package ingest.harvest

case class Start()  
    
case class QueryProgress()

case class ReportProgress(progress: Double, message: Option[String] = None)

case class Stopped(success: Boolean, message: Option[String] = None)