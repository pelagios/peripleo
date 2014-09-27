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
  
  def search(query: String, offset: Int = 0, limit: Int = 20, fuzzy: Boolean = false): Page[IndexedObject] = { 
    val searcherAndTaxonomy = searcherTaxonomyMgr.acquire()
    val searcher = new IndexSearcher(new MultiReader(searcherAndTaxonomy.searcher.getIndexReader, placeIndexReader))
    val taxonomyReader = searcherAndTaxonomy.taxonomyReader
    
    try {
      val fields = Seq(IndexFields.TITLE, IndexFields.DESCRIPTION, IndexFields.PLACE_NAME).toArray       
      val q = new MultiFieldQueryParser(Version.LUCENE_CURRENT, fields, analyzer).parse(query)
      
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
