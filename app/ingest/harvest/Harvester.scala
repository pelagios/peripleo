package ingest.harvest

import models.core.Dataset
import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor.Props
import akka.actor.Actor
import java.util.UUID
import play.api.Logger
import scala.collection.mutable.ArrayBuffer
import akka.actor.ActorRef

class HarvestActor(harvestId: UUID, voidURL: String, previous: Seq[Dataset]) extends Actor {
  
  def receive = {
    case "test" => Logger.info("received test")
    case _      => Logger.info("received unknown message")
  }
  
}
object Harvester {
  
  private val actors = new scala.collection.mutable.HashSet[ActorRef]
    
  def runHarvest(voidURL: String, previous: Seq[Dataset] = Seq.empty[Dataset]) = {
    val harvestId = UUID.randomUUID()
    val props = Props(classOf[HarvestActor], harvestId, voidURL, previous)
    actors.add(Akka.system.actorOf(props, harvestId.toString))    
  }
  
}
