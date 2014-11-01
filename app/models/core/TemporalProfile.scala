package models.core

import controllers.common.JSONWrites._
import play.api.libs.json.Json

class TemporalProfile(data: Seq[(Int, Int)]) {
  
  val histogram = data.foldLeft(scala.collection.mutable.Map.empty[Int, Int]) { case (h, (nextStart, nextEnd)) =>
    Seq.range(nextStart, nextEnd + 1).foreach(year => h.put(year, h.get(year).getOrElse(0) + 1))
    h
  }.toMap
  
  val maxValue = histogram.map(_._2).max
  
  val boundsStart = histogram.map(_._1).min
  
  val boundsEnd = histogram.map(_._1).max
  
  lazy val asJSON = Json.toJson(this)
  
  override lazy val toString = Json.stringify(asJSON)

}
