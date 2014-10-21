package index

case class BoundingBox(minLon: Double, maxLon: Double, minLat: Double, maxLat: Double)

object BoundingBox {
  
  def fromString(s: String): Option[BoundingBox] = {
    val coords = s.split(",").map(_.trim)
    if (coords.size == 4) {
      try {
        Some(BoundingBox(coords(0).toDouble, coords(1).toDouble, coords(2).toDouble, coords(3).toDouble))
      } catch {
        case _:Throwable => None
      }
    } else {
      None
    }
  }
  
}