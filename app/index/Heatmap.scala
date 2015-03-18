package index

class Heatmap(val cells: Seq[(Double, Double, Int)]) {
  
  def +(other: Heatmap): Heatmap = {
    val combined = 
      (cells ++ other.cells) // concatenate
        .groupBy(t => (t._1, t._2)) // group by (lon, lat)
        .map { case (lonLat, tuple) => (lonLat._1, lonLat._2, tuple.map(_._3).sum) } // sum per cell
        .toSeq
   
    Heatmap(combined)
  }
  
  def isEmpty = cells.isEmpty
  
}

object Heatmap {

  def apply(cells: Seq[(Double, Double, Int)]) = new Heatmap(cells)
  
  def empty = new Heatmap(Seq.empty[(Double, Double, Int)])
  
}
