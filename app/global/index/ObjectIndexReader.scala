package global.index

import models.Page
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.{ IndexSearcher, TopScoreDocCollector }
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.util.Version

trait ObjectIndexReader extends ObjectIndexBase {
  
  def search(query: String, offset: Int = 0, limit: Int = 20): Page[IndexedObject] = {
    val searcher = searcherManager.acquire()
    try {
      val fields = Seq(ObjectIndex.FIELD_TITLE, ObjectIndex.FIELD_DESCRIPTION).toArray    
      val q = new MultiFieldQueryParser(Version.LUCENE_47, fields, analyzer).parse(query)
    
      val collector = TopScoreDocCollector.create(offset + limit, true)
      searcher.search(q, collector)
    
      val total = collector.getTotalHits
      val results = collector.topDocs(offset, limit).scoreDocs.map(scoreDoc => new IndexedObject(searcher.doc(scoreDoc.doc)))
      Page(results.toSeq, offset, limit, total)
    } finally {
      searcherManager.release(searcher)
    } 
  }

}