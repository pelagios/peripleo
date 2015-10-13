package ingest.harvest

import akka.actor.{ Actor, ActorRef, Props }
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import scala.concurrent.duration._

class GazetteerImportActor(dataDumpPath: String, gazetteerName: String) extends Actor {
  
  def receive = {
    
    case Start => {
      new GazetteerImportWorker().importGazetteer(dataDumpPath, gazetteerName)
      sender ! Stopped(true)
    }
    
  }
  
}

object GazetteerImporter {
  
  private val actors = new scala.collection.mutable.HashSet[ActorRef]
  
  def importPlaces(dataDumpPath: String, gazetteerName: String) = {
    val props = Props(classOf[GazetteerImportActor], dataDumpPath, gazetteerName)
    val actor = Akka.system.actorOf(props) 
    actors.add(actor)
    
    ask(actor, Start)(Timeout(6 hours)) onSuccess {
      case s: Stopped => {
        actors.remove(actor)
      }
    }
  }
  
}