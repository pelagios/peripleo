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
    * @param query the query
    * @param offset the offset in the search result list
    * @param limit the size of the search result page
    * @fObjectType object type filter - only results of the specified type are returned
    * @fDataset dataset filter - only results from the specified dataset are returned (applicable for fObjectType == ANNOTATED_THING)
    * @fPlaces place filter - only results referencing the places are returned (applicable for fObjectType == ANNOTATED_THING | DATASET)
    */
  def search(query: String, offset: Int = 0, limit: Int = 20, fObjectType: Option[IndexedObjectTypes.Value] = None,
             fDataset: Option[String] = None, fPlaces: Seq[String] = Seq.empty[String]): Page[IndexedObject] = {
    
    val fields = Seq(IndexFields.TITLE, IndexFields.DESCRIPTION, IndexFields.PLACE_NAME).toArray       
    execute(new MultiFieldQueryParser(Version.LUCENE_4_9, fields, analyzer).parse(query))
  }
  
  def filter(offset: Int = 0, limit: Int = 20, fObjectType: Option[IndexedObjectTypes.Value] = None,
             fDataset: Option[String] = None, fPlaces: Seq[String] = Seq.empty[String]): Page[IndexedObject] = {

    val query = new BooleanQuery()
    
    if (fObjectType.isDefined)
      query.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, fObjectType.get.toString)), BooleanClause.Occur.MUST)
    
    if (fDataset.isDefined)
      query.add(new TermQuery(new Term(IndexFields.DATASET, fDataset.get)), BooleanClause.Occur.MUST)
      
    fPlaces.foreach(uri =>
      query.add(new TermQuery(new Term(IndexFields.PLACE_URI, uri)), BooleanClause.Occur.MUST))
      
    execute(query)
  }
  
  private def execute(query: Query, offset: Int = 0, limit: Int = 20, queryString: Option[String] = None): Page[IndexedObject] = {
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
