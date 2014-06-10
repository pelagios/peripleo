package index.places

import index.{ IndexBase, Index, IndexFields }
import org.apache.lucene.index.Term
import org.apache.lucene.search.{ BooleanClause, BooleanQuery, MatchAllDocsQuery, TermQuery, TopScoreDocCollector }

trait PlaceReader extends IndexBase {
  
  def listAllPlaces(offset: Int = 0, limit: Int = 20): Seq[IndexedPlace] = {
    val searcher = newPlaceSearcher()
    val collector = TopScoreDocCollector.create(offset + limit, true)
    searcher.search(new MatchAllDocsQuery(), collector)
    
    collector.topDocs(offset, limit).scoreDocs
      .map(scoreDoc => new IndexedPlace(searcher.doc(scoreDoc.doc))).toSeq
  }
  
  def findPlaceByURI(uri: String): Option[IndexedPlace] = {
    val q = new BooleanQuery()
    q.add(new TermQuery(new Term(IndexFields.PLACE_URI, Index.normalizeURI(uri))), BooleanClause.Occur.MUST)
    
    val searcher = newPlaceSearcher()
    val collector = TopScoreDocCollector.create(1, true)
    searcher.search(q, collector)
    
    val places = collector.topDocs.scoreDocs.map(scoreDoc => new IndexedPlace(searcher.doc(scoreDoc.doc)))
    if (places.size > 0)
      return Some(places(0))
    else
      None
  }

  def findPlaceByCloseMatch(uri: String): Seq[IndexedPlace] = {
    val q = new BooleanQuery()
    q.add(new TermQuery(new Term(IndexFields.PLACE_CLOSE_MATCH, Index.normalizeURI(uri))), BooleanClause.Occur.MUST)
    
    val searcher = newPlaceSearcher()
    val numHits = Math.max(1, numPlaces) // Has to be minimum 1, but can never exceed size of index
    val collector = TopScoreDocCollector.create(numHits, true)
    searcher.search(q, collector)
    
    collector.topDocs.scoreDocs.map(scoreDoc => new IndexedPlace(searcher.doc(scoreDoc.doc)))
  }
  
}