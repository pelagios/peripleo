package index.objects

import com.spatial4j.core.distance.DistanceUtils
import com.vividsolutions.jts.geom.Coordinate
import index._
import models.Page
import models.core.Datasets
import models.geo.BoundingBox
import org.apache.lucene.util.Version
import org.apache.lucene.index.{ Term, MultiReader }
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.search._
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.spatial.query.{ SpatialArgs, SpatialOperation }
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.search.spell.LuceneDictionary
import play.api.db.slick._
import scala.collection.JavaConverters._
import play.api.Logger
import org.apache.lucene.search.suggest.analyzing.FreeTextSuggester
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.spell.SpellChecker

trait ObjectReader extends IndexBase {

  /** Search the index.
    *  
    * @param limit search result page size
    * @param offset search result page offset
    * @query query keyword query
    * @query objectType filter search to a specific object type ('PLACE', 'ANNOTATED_THING' or 'DATASET')
    * @query dataset filter search to items in a specific dataset
    * @query places filter search to items referencing specific places 
    */
  def search(limit: Int, offset: Int, query: Option[String], objectType: Option[IndexedObjectTypes.Value] = None, 
      dataset: Option[String] = None, places: Seq[String] = Seq.empty[String], fromYear: Option[Int] = None, toYear: Option[Int] = None,
      bbox: Option[BoundingBox] = None, coord: Option[Coordinate] = None, radius: Option[Double] = None)(implicit s: Session): (Page[IndexedObject], FacetTree) = {
        
    val q = new BooleanQuery()
    
    // Keyword query
    if (query.isDefined) {
      val fields = Seq(IndexFields.TITLE, IndexFields.DESCRIPTION, IndexFields.PLACE_NAME).toArray       
      q.add(new MultiFieldQueryParser(fields, analyzer).parse(query.get), BooleanClause.Occur.MUST)  
    } 
      
    // Object type filter
    if (objectType.isDefined)
      q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, objectType.get.toString)), BooleanClause.Occur.MUST)
    
    // Dataset filter
    if (dataset.isDefined) {
      val datasetHierarchy = dataset.get +: Datasets.listSubsetsRecursive(dataset.get)
      if (datasetHierarchy.size == 1) {
        q.add(new TermQuery(new Term(IndexFields.ITEM_DATASET, dataset.get)), BooleanClause.Occur.MUST)        
      } else {
        val datasetQuery = new BooleanQuery()
        datasetHierarchy.foreach(id => {
          datasetQuery.add(new TermQuery(new Term(IndexFields.ITEM_DATASET, id)), BooleanClause.Occur.SHOULD)       
        })
        q.add(datasetQuery, BooleanClause.Occur.MUST)
      }
    }
      
    // Places filter
    places.foreach(uri =>
      q.add(new TermQuery(new Term(IndexFields.ITEM_PLACES, uri)), BooleanClause.Occur.MUST))
      
    // Timespan filter
    if (fromYear.isDefined || toYear.isDefined) {
      val timeIntervalQuery = new BooleanQuery()
      
      if (fromYear.isDefined)
        timeIntervalQuery.add(NumericRangeQuery.newIntRange(IndexFields.DATE_TO, fromYear.get, null, true, true), BooleanClause.Occur.MUST)
        
      if (toYear.isDefined)
        timeIntervalQuery.add(NumericRangeQuery.newIntRange(IndexFields.DATE_FROM, null, toYear.get, true, true), BooleanClause.Occur.MUST)
        
      q.add(timeIntervalQuery, BooleanClause.Occur.MUST)
    }
    
    // Spatial filter
    if (bbox.isDefined) {
      val rectangle = Index.spatialCtx.makeRectangle(bbox.get.minLon, bbox.get.maxLon, bbox.get.minLat, bbox.get.maxLat)
      q.add(Index.spatialStrategy.makeQuery(new SpatialArgs(SpatialOperation.IsWithin, rectangle)), BooleanClause.Occur.MUST)
    } else if (coord.isDefined) {
      // Warning - there appears to be a bug in Lucene spatial that flips coordinates!
      val circle = Index.spatialCtx.makeCircle(coord.get.y, coord.get.x, DistanceUtils.dist2Degrees(radius.getOrElse(10), DistanceUtils.EARTH_MEAN_RADIUS_KM))
      q.add(Index.spatialStrategy.makeQuery(new SpatialArgs(SpatialOperation.IsWithin, circle)), BooleanClause.Occur.MUST)        
    }
      
    execute(q, limit, offset, query)
  }
  
  private def execute(query: Query, limit: Int, offset: Int, queryString: Option[String]): (Page[IndexedObject], FacetTree) = {
    val placeSearcher = placeSearcherManager.acquire()
    val objectSearcher = objectSearcherManager.acquire()
    val searcher = new IndexSearcher(new MultiReader(objectSearcher.searcher.getIndexReader, placeSearcher.searcher.getIndexReader))
    val taxonomyReader = objectSearcher.taxonomyReader
    
    try {      
      val facetsCollector = new FacetsCollector()
      val topDocsCollector = TopScoreDocCollector.create(offset + limit, true)
      searcher.search(query, MultiCollector.wrap(topDocsCollector, facetsCollector))
      
      val facetTree = new FacetTree(new FastTaxonomyFacetCounts(taxonomyReader, Index.facetsConfig, facetsCollector))      
       
      val total = topDocsCollector.getTotalHits
      val results = topDocsCollector.topDocs(offset, limit).scoreDocs.map(scoreDoc => new IndexedObject(searcher.doc(scoreDoc.doc)))
      (Page(results.toSeq, offset, limit, total, queryString), facetTree)
    } finally {
      placeSearcherManager.release(placeSearcher)
      objectSearcherManager.release(objectSearcher)
    }     
  }

}
