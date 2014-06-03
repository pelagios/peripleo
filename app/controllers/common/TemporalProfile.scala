package controllers.common

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

class TemporalProfile(data: Seq[(Int, Int)]) {
    
  val histogram = data.foldLeft(Map.empty[Int, Int]){ case (h, (nextStart, nextEnd)) =>
    h.map {
      case (year, count) if (year >= nextStart && year <= nextEnd) => (year, count + 1)
      case (year, count) => (year, count)
    }
  }
  
  val maxValue = histogram.map(_._2).max
  
  val boundsStart = histogram.map(_._1).min
  
  val boundsEnd = histogram.map(_._1).max
  
  lazy val asJSON = Json.toJson(this)
  
  override lazy val toString = Json.stringify(asJSON)

}

object TemporalProfile {

  implicit val histogramWrites: Writes[(Int, Int)] = (
    (JsPath \ "year").write[Int] ~
    (JsPath \ "value").write[Int]
  )(tuple => (
      tuple._1,
      tuple._2))
  
  implicit val profileWrites: Writes[TemporalProfile] = (
    (JsPath \ "bounds_start").write[Int] ~
    (JsPath \ "bounds_end").write[Int] ~
    (JsPath \ "max_value").write[Int] ~
    (JsPath \ "histogram").write[Seq[(Int, Int)]]
  )(profile => (
      profile.boundsStart,
      profile.boundsEnd,
      profile.maxValue,
      profile.histogram.toSeq.sortBy(_._1)))
  
}