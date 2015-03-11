package index

import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import scala.collection.JavaConverters._
import play.api.Logger

/** A helper datastructure for easier access to search result facets **/
class FacetTree(facetCounts: FastTaxonomyFacetCounts) {

  def dimensions(limit: Int = 10): Seq[String] =
    facetCounts.getAllDims(limit).asScala.map(_.dim).toSeq
  
  def getTopChildren(dimension: String, limit: Int = 10, path: Seq[String] = Seq.empty[String]): Seq[(String, Int)] =
    Option(facetCounts.getTopChildren(limit, dimension, path:_*)).map(result =>
      result.labelValues.toSeq.map(lv => (lv.label, lv.value.intValue))).getOrElse(Seq.empty[(String, Int)])

}