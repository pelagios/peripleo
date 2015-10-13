package ingest.harvest

import akka.actor.{ Actor, ActorRef, Props }
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import scala.concurrent.duration._

class GazetteerImportActor(path: String, gazetteerName: String, origFilename: Option[String] = None) extends Actor {
  
  def receive = {
    
    case Start => {
      new GazetteerImportWorker().importDataDump(path, gazetteerName, origFilename)
      sender ! Stopped(true)
    }
    
    case QueryProgress => {
      // TODO measure progress (somehow)
      sender ! ReportProgress(0.5)
    }
    
  }
  
}

object GazetteerImporter {
  
  private val actors = new scala.collection.mutable.HashSet[ActorRef]
  
  def importDataDump(path: String, gazetteerName: String, origFilename: Option[String]) = {
    val props = Props(classOf[GazetteerImportActor], path, gazetteerName, origFilename)
    val actor = Akka.system.actorOf(props) 
    actors.add(actor)
    
    ask(actor, Start)(Timeout(6 hours)) onSuccess {
      case s: Stopped => {
        actors.remove(actor)
      }
    }
  }
  
}