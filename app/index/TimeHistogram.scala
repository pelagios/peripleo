package index

class TimeHistogram private (val values: Seq[(Int, Int)]) {
  
  val startYear = values.headOption.map(_._1).getOrElse(0)
  
  val endYear = values.lastOption.map(_._1).getOrElse(0)
  
  lazy val maxCount = values.map(_._2).max

}

object TimeHistogram {
  
  def create(vals: Seq[(Int, Int)], maxBuckets: Int = -1): TimeHistogram = {
    if (vals.size > 0) {
      val values = 
        if (maxBuckets < 0) {
          vals.sortBy(_._1)
        } else {
          val stepSize = Math.ceil(vals.size.toDouble / maxBuckets).toInt
          vals.sortBy(_._1).grouped(stepSize).map(values => (values.head._1, values.map(_._2).sum)).toSeq
        }
        
      new TimeHistogram(values)
    } else {
      new TimeHistogram(Seq.empty[(Int, Int)])
    }
  }
  
}
