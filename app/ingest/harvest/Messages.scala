package ingest.harvest

case class Start()  
    
case class QueryProgress()

case class ReportProgress(progress: Double, message: String)

case class Stopped(success: Boolean, message: Option[String] = None)