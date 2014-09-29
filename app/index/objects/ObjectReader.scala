package index.objects

import index._
import models.Page
import org.apache.lucene.util.Version
import org.apache.lucene.index.MultiReader
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.{ IndexSearcher, MultiCollector, TopScoreDocCollector }
import org.apache.lucene.sandbox.queries.DuplicateFilter
import org.apache.lucene.index.SlowCompositeReaderWrapper

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
  def search(query: String, offset: Int = 0, limit: Int = 20, fOjectType: Option[IndexedObjectTypes.Value] = None,
             fDataset: Option[String] = None, fPlaces: Seq[String] = Seq.empty[String]): Page[IndexedObject] = {
    
    val searcherAndTaxonomy = searcherTaxonomyMgr.acquire()
    val searcher = new IndexSearcher(new MultiReader(searcherAndTaxonomy.searcher.getIndexReader, placeIndexReader))
    val taxonomyReader = searcherAndTaxonomy.taxonomyReader
    
    try {
      val fields = Seq(IndexFields.TITLE, IndexFields.DESCRIPTION, IndexFields.PLACE_NAME).toArray       
      val q = new MultiFieldQueryParser(Version.LUCENE_4_9, fields, analyzer).parse(query)
      
      val facetsCollector = new FacetsCollector()
      val topDocsCollector = TopScoreDocCollector.create(offset + limit, true)          
      searcher.search(q, MultiCollector.wrap(topDocsCollector, facetsCollector))
      
      val facets = new FastTaxonomyFacetCounts(taxonomyReader, facetsConfig, facetsCollector)
      val facetResults = Seq(facets.getTopChildren(offset + limit, IndexFields.OBJECT_TYPE))
      
      val total = topDocsCollector.getTotalHits
      val results = topDocsCollector.topDocs(offset, limit).scoreDocs.map(scoreDoc => new IndexedObject(searcher.doc(scoreDoc.doc)))
      Page(results.toSeq, offset, limit, total, Some(query))
    } finally {
      searcherTaxonomyMgr.release(searcherAndTaxonomy)
    } 
  }

}
