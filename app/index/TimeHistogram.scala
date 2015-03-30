package index

class TimeHistogram private (val values: Seq[(Int, Int)]) {
  
  val startYear = values.headOption.map(_._1).getOrElse(0)
  
  val endYear = values.lastOption.map(_._1).getOrElse(0)
  
  lazy val maxCount = values.map(_._2).max

}

object TimeHistogram {
  
  def create(vals: Seq[(Int, Int)], maxBuckets: Int = -1): TimeHistogram = {    
    if (vals.size > 0) {
      // Lucene delivers a sparse result - so we need to fill in the empty cells before resampling
      val paddedValues = vals.sortBy(_._1).foldLeft(Seq.empty[(Int, Int)]) { case (padded, (year, count)) => {
        padded.lastOption match {
          case Some(previous) => {
            if (previous._1 == year - 1) // Nothing missing - no need to apd
              padded :+ (year, count) 
            else // Pad cells in between previous and this year
              padded ++ Seq.range(previous._1 + 1, year).map((_, 0)) :+ (year, count)
          }
          
          case None =>  Seq((year, count))
        }
      }}
      
      val resampledValues = 
        if (maxBuckets < 0) {
          paddedValues
        } else {
          val stepSize = Math.ceil(paddedValues.size.toDouble / maxBuckets).toInt
          paddedValues.grouped(stepSize).map(values => (values.last._1, values.map(_._2).sum / values.size)).toSeq
        }
        
      new TimeHistogram(resampledValues)
    } else {
      new TimeHistogram(Seq.empty[(Int, Int)])
    }
  }
  
}
