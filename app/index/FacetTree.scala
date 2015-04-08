package index

import models.core.Dataset
import org.apache.lucene.facet.Facets
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import scala.collection.JavaConverters._

/** A helper datastructure for easier access to search result facets **/
class FacetTree(facetCounts: Facets) {

  def dimensions(limit: Int = 10): Seq[String] =
    facetCounts.getAllDims(limit).asScala.map(_.dim).toSeq
  
  def getTopChildren(dimension: String, limit: Int = 5, path: Seq[String] = Seq.empty[String]): Seq[(String, Int)] =
    Option(facetCounts.getTopChildren(limit, dimension, path:_*)).map(result =>
      result.labelValues.toSeq.map(lv => (lv.label, lv.value.intValue))).getOrElse(Seq.empty[(String, Int)])

}

