package index

class Heatmap(val cells: Seq[(Double, Double, Int)], val cellWidth: Double, val cellHeight: Double) {
  
  private lazy val values = cells.map(_._3)
  
  lazy val maxValue = values.max
  
  lazy val minValue = values.min
  
  def +(other: Heatmap): Heatmap = {
    val combined = 
      (cells ++ other.cells) // concatenate
        .groupBy(t => (t._1, t._2)) // group by (lon, lat)
        .map { case (lonLat, tuple) => (lonLat._1, lonLat._2, tuple.map(_._3).sum) } // sum per cell
        .toSeq
   
    Heatmap(combined, cellWidth, cellHeight) // TODO throw exception if cell dimensions differ!
  }
  
  def isEmpty = cells.isEmpty
  
}

object Heatmap {

  def apply(cells: Seq[(Double, Double, Int)], cellWidth: Double, cellHeight: Double) = new Heatmap(cells, cellWidth, cellHeight)
  
  def empty = new Heatmap(Seq.empty[(Double, Double, Int)], 0, 0)
  
}
