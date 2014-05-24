package global.index

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.util.Version
import org.apache.lucene.search.TopScoreDocCollector

trait ObjectIndexReader extends ObjectIndexBase {
  
  def search(query: String, numHits: Int): Seq[IndexedObject] = {
    val searcher = searcherManager.acquire()
    try {
      val fields = Seq(ObjectIndex.FIELD_TITLE, ObjectIndex.FIELD_DESCRIPTION).toArray    
      val q = new MultiFieldQueryParser(Version.LUCENE_47, fields, analyzer).parse(query)
    
      val collector = TopScoreDocCollector.create(numHits, true)
      searcher.search(q, collector)
    
      collector.topDocs.scoreDocs.map(scoreDoc => new IndexedObject(searcher.doc(scoreDoc.doc)))
    } finally {
      searcherManager.release(searcher)
    } 
  }

}