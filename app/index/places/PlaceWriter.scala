package index.places

import index.{ Index, IndexFields }
import java.io.InputStream
import org.apache.lucene.document.{ Field, StringField }
import org.apache.lucene.index.{ IndexWriter, Term }
import org.apache.lucene.search.{ BooleanQuery, BooleanClause, TermQuery, TopScoreDocCollector }
import org.pelagios.Scalagios
import org.pelagios.api.gazetteer.Place
import play.api.Logger
import scala.collection.mutable.Set

trait PlaceWriter extends PlaceReader {
  
  def addPlaces(places: Iterator[Place], sourceGazetteer: String): (Int, Seq[String]) =  { 
    val writer = newPlaceWriter()
    
    val uriPrefixes = Set.empty[String]
    val distinctNewPlaces = places.foldLeft(0)((distinctNewPlaces, place) => {
      val isDistinct = addPlace(place, sourceGazetteer, uriPrefixes, writer)
      if (isDistinct)
        distinctNewPlaces + 1 
      else
        distinctNewPlaces
    })

    writer.close()
    (distinctNewPlaces, uriPrefixes.toSeq)
  }
  
  def addPlaceStream(is: InputStream, filename: String, sourceGazetteer: String): (Int, Int, Seq[String]) = {
    val writer = newPlaceWriter()
    
    val uriPrefixes = Set.empty[String]
    var totalPlaces = 0
    var distinctNewPlaces = 0
    def placeHandler(place: Place): Unit = {
      val isDistinct = addPlace(place, sourceGazetteer, uriPrefixes, writer)
      totalPlaces += 1
      if (isDistinct)
        distinctNewPlaces += 1
    }
    
    Scalagios.streamPlaces(is, filename, placeHandler)
    writer.close()
    (totalPlaces, distinctNewPlaces, uriPrefixes.toSeq)
  }
  
  private def addPlace(place: Place, sourceGazetteer: String, uriPrefixes: Set[String], writer: IndexWriter): Boolean = {
      val normalizedUri = Index.normalizeURI(place.uri)
      
      // Enforce uniqueness
      if (findNetworkByPlaceURI(normalizedUri).isDefined) {
        Logger.warn("Place '" + place.uri + "' already in index!")
        false // No new distinct place
      } else {
        // Record URI prefix
        uriPrefixes.add(normalizedUri.substring(0, normalizedUri.indexOf('/', 8)))
            
        // First, we query our index for all closeMatches our new place has 
        val closeMatches = place.closeMatches.map(uri => {
          val normalized = Index.normalizeURI(uri)
          (normalized, findNetworkByPlaceURI(normalized))
        })
        
        // These are the closeMatches we already have in our index        
        val indexedCloseMatchesOut = closeMatches.filter(_._2.isDefined).map(_._2.get)

        // Next, we query our index for places which list our new places as their closeMatch
        val indexedCloseMatchesIn = findNetworkByCloseMatch(normalizedUri)
        
        val indexedCloseMatches = indexedCloseMatchesOut ++ indexedCloseMatchesIn
        
        // These are closeMatch URIs we don't have in our index (yet)...
        val unrecordedCloseMatchesOut = closeMatches.filter(_._2.isEmpty).map(_._1)

        // ...but we can still use them to extend our network through indirect connections
        val indirectlyConnectedPlaces = 
          unrecordedCloseMatchesOut.flatMap(uri => findNetworkByCloseMatch(uri))
          .filter(!indexedCloseMatches.contains(_)) // We filter out places that are already connected directly

        val allCloseMatches = (indexedCloseMatches ++ indirectlyConnectedPlaces).distinct

        // Update the index
        updateIndex(IndexedPlace.toIndexedPlace(place, sourceGazetteer), allCloseMatches, writer);
        
        // If this place didn't have any closeMatches at all, it's a new distinct contribution
        allCloseMatches.size == 0
      }      
  }
  
  private def updateIndex(place: IndexedPlace, affectedNetworks: Seq[IndexedPlaceNetwork], writer: IndexWriter) = {
    // Delete affected networks from index
    affectedNetworks.foreach(network => 
      writer.deleteDocuments(new TermQuery(new Term(IndexFields.PLACE_URI, network.seedURI))))

    // Add the place and write updated network to index
    val updatedNetwork = IndexedPlaceNetwork.join(place, affectedNetworks)
    writer.addDocument(updatedNetwork.doc)
  }

}