package index.places

import index.{ IndexBase, Index, IndexFields }
import org.apache.lucene.index.Term
import org.apache.lucene.search.{ BooleanClause, BooleanQuery, MatchAllDocsQuery, TermQuery, TopScoreDocCollector }
import play.api.Logger

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
  
  def getNetwork(place: IndexedPlace): PlaceNetwork = {
    val q = new BooleanQuery()
    q.add(new TermQuery(new Term(IndexFields.PLACE_SEED_URI, place.seedURI)), BooleanClause.Occur.MUST)
    
    val searcher = newPlaceSearcher()    
    val numHits = Math.max(1, numPlaces) // Has to be minimum 1, but can never exceed size of index
    val collector = TopScoreDocCollector.create(numHits, true)
    searcher.search(q, collector)
    
    // These are all the places in the network we have in our index
    val places = collector.topDocs.scoreDocs.map(scoreDoc => new IndexedPlace(searcher.doc(scoreDoc.doc))).toSeq
    val placeURIs = places.map(_.uri)

    // These are additional closeMatch URIS we *do not* have in the index
    // But we can still use the info to extend the network through indirect connections
    val additionalCloseMatches = places.flatMap(_.closeMatches).filter(!placeURIs.contains(_))
    val indirectlyConnectedPlaces = expandNetwork(additionalCloseMatches).filter(p => !placeURIs.contains(p.uri))
    
    if (indirectlyConnectedPlaces.size > 0) {
      Logger.info("Found additional network members through indirect connections:")
      indirectlyConnectedPlaces.foreach(p => Logger.info("  " + p.title))
    }
    
    val allPlaces = places ++ indirectlyConnectedPlaces
        
    // List of edges fromURI -> toURI
    val edges = allPlaces.flatMap(p => Seq.fill(p.closeMatches.size)(p.uri).zip(p.closeMatches))
    PlaceNetwork((places ++ indirectlyConnectedPlaces).distinct, edges)
  }
  
  private def expandNetwork(uris: Seq[String]): Seq[IndexedPlace] = {
    val q = new BooleanQuery()
    uris.foreach(uri => q.add(new TermQuery(new Term(IndexFields.PLACE_CLOSE_MATCH, Index.normalizeURI(uri))), BooleanClause.Occur.SHOULD))
    
    val searcher = newPlaceSearcher()    
    val numHits = Math.max(1, numPlaces) // Has to be minimum 1, but can never exceed size of index
    val collector = TopScoreDocCollector.create(numHits, true)
    searcher.search(q, collector)
    
    // These are all places which have any of the provided closeMatch URIs
    collector.topDocs.scoreDocs.map(scoreDoc => new IndexedPlace(searcher.doc(scoreDoc.doc))).toSeq
  }
  
}