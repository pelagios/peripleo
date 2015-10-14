package ingest.harvest

import akka.actor.{ Actor, ActorRef, Props }
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import global.Global
import play.api.Logger

class GazetteerImportActor(path: String, gazetteerName: String, origFilename: Option[String] = None) extends Actor {
  
  val worker = new GazetteerImportWorker(Global.index)
  
  def receive = {
    
    case Start => {
      worker.importDataDump(path, gazetteerName, origFilename)
      sender ! Stopped(true)
    }
    
    case QueryProgress => {
      sender ! ReportProgress(worker.getProgress())
    }
    
  }
  
}

object GazetteerImporter {
  
  private val actors = new scala.collection.mutable.HashMap[String, ActorRef]
  
  def getProgress(gazetteerName: String): Double = {
    val actorRef = actors.get(gazetteerName.toLowerCase)
    if (actorRef.isDefined) {
      implicit val timeout = new Timeout(Duration.create(30, "seconds"))
      val f = actorRef.get ? QueryProgress
      val result = Await.result(f, timeout.duration).asInstanceOf[ReportProgress]
      Logger.info("yo.")
      result.progress
    } else {
      Logger.info("actor ref not found")
      0
    }
  }
    
  def importDataDump(path: String, gazetteerName: String, origFilename: Option[String]) = {
    val props = Props(classOf[GazetteerImportActor], path, gazetteerName, origFilename)
    val actor = Akka.system.actorOf(props) 
    actors.put(gazetteerName.toLowerCase, actor)
    
    ask(actor, Start)(new Timeout(Duration.create(6, "hours"))) onSuccess {
      case s: Stopped => {
        actors.remove(gazetteerName)
      }
    }
  }
  
}