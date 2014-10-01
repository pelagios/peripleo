package index.objects

import index._
import models.{ Datasets, Page }
import org.apache.lucene.util.Version
import org.apache.lucene.index.{ Term, MultiReader }
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.{ BooleanQuery, BooleanClause, IndexSearcher, MultiCollector, NumericRangeQuery, Query, TermQuery, TopScoreDocCollector }
import play.api.db.slick._

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
      dataset: Option[String] = None, places: Seq[String] = Seq.empty[String], fromYear: Option[Int], toYear: Option[Int])(implicit s: Session): Page[IndexedObject] = {
    
    val q = new BooleanQuery()
    
    // Keyword query
    if (query.isDefined) {
      val fields = Seq(IndexFields.TITLE, IndexFields.DESCRIPTION, IndexFields.PLACE_NAME).toArray       
      q.add(new MultiFieldQueryParser(Version.LUCENE_4_9, fields, analyzer).parse(query.get), BooleanClause.Occur.MUST)  
    } 
      
    // Object type filter
    if (objectType.isDefined) {
      if (objectType.get == IndexedObjectTypes.PLACE) {
        q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString)), BooleanClause.Occur.MUST_NOT)
        q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, IndexedObjectTypes.ANNOTATED_THING.toString)), BooleanClause.Occur.MUST_NOT)
      } else {
        q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, objectType.get.toString)), BooleanClause.Occur.MUST)
      }
    }
    
    // Dataset filter
    if (dataset.isDefined) {
      val datasetHierarchy = dataset.get +: Datasets.listSubsetsRecursive(dataset.get)
      if (datasetHierarchy.size == 1) {
        q.add(new TermQuery(new Term(IndexFields.DATASET, dataset.get)), BooleanClause.Occur.MUST)        
      } else {
        val datasetQuery = new BooleanQuery()
        datasetHierarchy.foreach(id => {
          datasetQuery.add(new TermQuery(new Term(IndexFields.DATASET, id)), BooleanClause.Occur.SHOULD)       
        })
        q.add(datasetQuery, BooleanClause.Occur.MUST)
      }
    }
      
    // Places filter
    places.foreach(uri =>
      q.add(new TermQuery(new Term(IndexFields.PLACE_URI, uri)), BooleanClause.Occur.MUST))
      
    // Timespan filter
    if (fromYear.isDefined && toYear.isDefined) {
      val timeIntervalQuery = new BooleanQuery()
      timeIntervalQuery.add(NumericRangeQuery.newIntRange(IndexFields.DATE_FROM, null, toYear.get, true, true), BooleanClause.Occur.MUST)
      timeIntervalQuery.add(NumericRangeQuery.newIntRange(IndexFields.DATE_TO, fromYear.get, null, true, true), BooleanClause.Occur.MUST)
      q.add(timeIntervalQuery, BooleanClause.Occur.MUST)
    }
      
    execute(q, limit, offset, query)
  }
  
  private def execute(query: Query, limit: Int, offset: Int, queryString: Option[String]): Page[IndexedObject] = {
    val searcherAndTaxonomy = searcherTaxonomyMgr.acquire()
    val searcher = new IndexSearcher(new MultiReader(searcherAndTaxonomy.searcher.getIndexReader, placeIndexReader))
    val taxonomyReader = searcherAndTaxonomy.taxonomyReader
    
    try {      
      val facetsCollector = new FacetsCollector()
      val topDocsCollector = TopScoreDocCollector.create(offset + limit, true)          
      searcher.search(query, MultiCollector.wrap(topDocsCollector, facetsCollector))
      
      val facets = new FastTaxonomyFacetCounts(taxonomyReader, facetsConfig, facetsCollector)
      val facetResults = Seq(facets.getTopChildren(offset + limit, IndexFields.OBJECT_TYPE))
      
      val total = topDocsCollector.getTotalHits
      val results = topDocsCollector.topDocs(offset, limit).scoreDocs.map(scoreDoc => new IndexedObject(searcher.doc(scoreDoc.doc)))
      Page(results.toSeq, offset, limit, total, queryString)
    } finally {
      searcherTaxonomyMgr.release(searcherAndTaxonomy)
    }     
  }

}
