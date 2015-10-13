package ingest.harvest

import akka.pattern.ask
import akka.util.Timeout
import java.util.UUID
import models.core.Dataset
import play.api.Play.current
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor.{ Actor, ActorRef, Props }
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

class DataHarvestActor(harvestId: UUID, voidURL: String, previous: Seq[Dataset]) extends Actor {
  
  def receive = {
    
    case Start => {
      new DataHarvestWorker().harvest(voidURL, previous)
      sender ! Stopped(true)
    }
    
  }
  
}

object DataHarvester {
  
  private val actors = new scala.collection.mutable.HashSet[ActorRef]
    
  def harvest(voidURL: String, previous: Seq[Dataset] = Seq.empty[Dataset]) = {    
    val harvestId = UUID.randomUUID()
    
    val props = Props(classOf[DataHarvestActor], harvestId, voidURL, previous)
    val actor = Akka.system.actorOf(props, harvestId.toString) 
    actors.add(actor)
    
    val startTime = System.currentTimeMillis
    ask(actor, Start)(Timeout(6 hours)) onSuccess {
      case s: Stopped => {
        Logger.info("Harvest " + harvestId + " complete - took " + (System.currentTimeMillis - startTime) + " ms")
        actors.remove(actor)
      }
    }
  }
  
}
