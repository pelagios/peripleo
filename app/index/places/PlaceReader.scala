package index.places

import scala.collection.JavaConversions._
import index.{ IndexBase, Index, IndexFields }
import org.apache.lucene.index.Term
import org.apache.lucene.search.{ BooleanClause, BooleanQuery, MatchAllDocsQuery, TermQuery, TopScoreDocCollector }
import play.api.Logger
import org.apache.lucene.search.PhraseQuery
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.standard.StandardAnalyzer

trait PlaceReader extends IndexBase {
  
  def listAllPlaceNetworks(offset: Int = 0, limit: Int = 20): Seq[IndexedPlaceNetwork] = {
    val searcher = newPlaceSearcher()
    val collector = TopScoreDocCollector.create(offset + limit, true)
    searcher.search(new MatchAllDocsQuery(), collector)
    
    collector.topDocs(offset, limit).scoreDocs
      .map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc))).toSeq
  }
  
  def listAllPlaces(gazetteer: String, offset: Int = 0, limit: Int = 20): Seq[IndexedPlace] = {    
    val query = new TermQuery(new Term(IndexFields.PLACE_SOURCE_GAZETTEER, gazetteer))
 
    val searcher = newPlaceSearcher()
    val collector = TopScoreDocCollector.create(offset + limit, true)
    searcher.search(query, collector)
    
    collector.topDocs(offset, limit).scoreDocs
      .map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc))).toSeq
      .map(_.places.filter(_.sourceGazetteer == gazetteer).head)
  }
  
  def findPlaceByURI(uri: String): Option[IndexedPlace] =
    findNetworkByPlaceURI(uri).flatMap(_.getPlace(uri))
    
  def findNetworkByPlaceURI(uri: String): Option[IndexedPlaceNetwork] = {
    val q = new TermQuery(new Term(IndexFields.PLACE_URI, Index.normalizeURI(uri)))
    
    val searcher = newPlaceSearcher()
    val collector = TopScoreDocCollector.create(1, true)
    searcher.search(q, collector)
    
    collector.topDocs.scoreDocs.map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc))).headOption
  }

  def findNetworkByCloseMatch(uri: String): Seq[IndexedPlaceNetwork] = {
    val q = new TermQuery(new Term(IndexFields.PLACE_CLOSE_MATCH, Index.normalizeURI(uri)))
    
    val searcher = newPlaceSearcher()
    val numHits = Math.max(1, numPlaceNetworks) // Has to be minimum 1, but can never exceed size of index
    val collector = TopScoreDocCollector.create(numHits, true)
    searcher.search(q, collector)
    
    collector.topDocs.scoreDocs.map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc)))
  }
  
}