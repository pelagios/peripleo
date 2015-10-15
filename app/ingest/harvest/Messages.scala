package ingest.harvest

case class Start()  

case class Stopped(success: Boolean, message: Option[String] = None)