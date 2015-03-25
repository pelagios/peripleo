package index

/** TODO add option to resample the histogram to a max number of buckets **/
class TimeHistogram(vals: Seq[(Int, Int)]) {
  
  val values = vals.sortBy(_._1)
  
  val startYear = values.head._1
  
  val endYear = values.last._1

}
