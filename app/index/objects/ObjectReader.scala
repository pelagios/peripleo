package index.objects

import index._
import models.Page
import org.apache.lucene.util.Version
import org.apache.lucene.index.{ Term, MultiReader }
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.{ BooleanQuery, BooleanClause, IndexSearcher, MultiCollector, TermQuery, TopScoreDocCollector }
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import play.api.Logger

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
      dataset: Option[String] = None, places: Seq[String] = Seq.empty[String]): Page[IndexedObject] = {
    
    val q = new BooleanQuery()
    
    if (query.isDefined) {
      val fields = Seq(IndexFields.TITLE, IndexFields.DESCRIPTION, IndexFields.PLACE_NAME).toArray       
      q.add(new MultiFieldQueryParser(Version.LUCENE_4_9, fields, analyzer).parse(query.get), BooleanClause.Occur.MUST)  
    } 
      
    if (objectType.isDefined) {
      if (objectType.get == IndexedObjectTypes.PLACE) {
        q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString)), BooleanClause.Occur.MUST_NOT)
        q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, IndexedObjectTypes.ANNOTATED_THING.toString)), BooleanClause.Occur.MUST_NOT)
      } else {
        q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, objectType.get.toString)), BooleanClause.Occur.MUST)
      }
    }
    
    if (dataset.isDefined)
      q.add(new TermQuery(new Term(IndexFields.DATASET, dataset.get)), BooleanClause.Occur.MUST)
      
    places.foreach(uri =>
      q.add(new TermQuery(new Term(IndexFields.PLACE_URI, uri)), BooleanClause.Occur.MUST))
      
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
