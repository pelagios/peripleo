package global.index

import models.Page
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.{ IndexSearcher, TopScoreDocCollector }
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.util.Version
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.search.MultiCollector

trait ObjectIndexReader extends ObjectIndexBase {
  
  def search(query: String, offset: Int = 0, limit: Int = 20): Page[IndexedObject] = {
    val searcherAndTaxonomy = taxonomyManager.acquire()
    val searcher = searcherAndTaxonomy.searcher
    val taxonomyReader = searcherAndTaxonomy.taxonomyReader
    
    try {
      val fields = Seq(ObjectIndex.FIELD_TITLE, ObjectIndex.FIELD_DESCRIPTION).toArray    
      val q = new MultiFieldQueryParser(Version.LUCENE_48, fields, analyzer).parse(query)
      
      val facetsCollector = new FacetsCollector()
      val topDocsCollector = TopScoreDocCollector.create(offset + limit, true)    
      searcher.search(q, MultiCollector.wrap(topDocsCollector, facetsCollector))
      
      val facets= new FastTaxonomyFacetCounts(taxonomyReader, config, facetsCollector)
      val facetResults = Seq(
        facets.getTopChildren(offset + limit, ObjectIndex.CATEGORY_DATASET),
        facets.getTopChildren(offset + limit, ObjectIndex.CATEGORY_ANNOTATED_THING))
        
      val total = topDocsCollector.getTotalHits
      val results = topDocsCollector.topDocs(offset, limit).scoreDocs.map(scoreDoc => new IndexedObject(searcher.doc(scoreDoc.doc)))
      Page(results.toSeq, offset, limit, total)
    } finally {
      taxonomyManager.release(searcherAndTaxonomy)
    } 
  }

}