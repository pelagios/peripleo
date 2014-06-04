package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

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

object TemporalProfile {
  
  implicit val profileWrites: Writes[TemporalProfile] = (
    (JsPath \ "bounds_start").write[Int] ~
    (JsPath \ "bounds_end").write[Int] ~
    (JsPath \ "max_value").write[Int] ~
    (JsPath \ "histogram").write[Map[String, Int]]
  )(profile => (
      profile.boundsStart,
      profile.boundsEnd,
      profile.maxValue,
      profile.histogram.map(t => (t._1.toString, t._2))))
  
}