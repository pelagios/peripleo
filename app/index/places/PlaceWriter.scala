package index.places

import index.{ Index, IndexFields }
import org.apache.lucene.document.{ Field, StringField }
import org.apache.lucene.index.{ IndexWriter, Term }
import org.apache.lucene.search.{ BooleanQuery, BooleanClause, TermQuery, TopScoreDocCollector }
import org.pelagios.api.gazetteer.Place
import play.api.Logger

trait PlaceWriter extends PlaceReader {
  
  def addPlaces(places: Iterable[Place], sourceGazetteer: String): Int =  { 
    val writer = newPlaceWriter()
    
    val distinctNewPlaces = places.map(place => {
      val normalizedUri = Index.normalizeURI(place.uri)
      
      // Enforce uniqueness
      if (findPlaceByURI(normalizedUri).isDefined) {
        Logger.warn("Place '" + place.uri + "' already in index!")
        0
      } else {
        // First, we query our index for all closeMatches our new place has 
        val closeMatches = place.closeMatches.map(uri => {
          val normalized = Index.normalizeURI(uri)
          (normalized, findPlaceByURI(normalized))
        })
        
        // These are the closeMatches we already have in our index        
        val indexedCloseMatchesOut = closeMatches.filter(_._2.isDefined).map(_._2.get)

        // Next, we query our index for places which list our new places as their closeMatch
        val indexedCloseMatchesIn = findPlaceByCloseMatch(normalizedUri)
        
        val indexedCloseMatches = indexedCloseMatchesOut ++ indexedCloseMatchesIn
        
        // These are closeMatch URIs we don't have in our index (yet)...
        val unrecordedCloseMatchesOut = closeMatches.filter(_._2.isEmpty).map(_._1)

        // ...but we can still use them to extend our network through indirect connections
        val indirectlyConnectedPlaces = // expandNetwork(unrecordedCloseMatchesOut)
          unrecordedCloseMatchesOut.flatMap(uri => findPlaceByCloseMatch(uri))
          .filter(!indexedCloseMatches.contains(_)) // We filter out places that are already connected directly

        if (indirectlyConnectedPlaces.size > 0) {
          Logger.info("Connecting " + indirectlyConnectedPlaces.size + " places through indirect closeMatches")
          indirectlyConnectedPlaces.foreach(p => Logger.info("  " + p.title))
        }

        val allCloseMatches = indexedCloseMatches ++ indirectlyConnectedPlaces
        
        // All closeMatches need to share the same seed URI
        val seedURI =
          if (allCloseMatches.size > 0) 
            allCloseMatches(0).seedURI
          else
            normalizedUri
 
        // Update seed URIs where necessary
        updateSeedURI(allCloseMatches.filter(!_.seedURI.equals(seedURI)), seedURI, writer)
        
        // Add new document to index
        val differentSeedURI = if (normalizedUri == seedURI) None else Some(seedURI)
        writer.addDocument(IndexedPlace.toDoc(place, sourceGazetteer, Some(seedURI)))
        
        // If this place didn't have any closeMatches in the index, it's counted as a new distinct contribution
        if (closeMatches.size == 0)
          1
        else
          0
      }   
    })
    
    writer.close()
    distinctNewPlaces.foldLeft(0)(_ + _)
  }
  
  def updateSeedURI(places: Seq[IndexedPlace], seedURI: String, writer: IndexWriter) = {
    places.foreach(place => {
      // Delete doc from index
      writer.deleteDocuments(new Term(IndexFields.PLACE_URI, place.uri))
      
      // Update seed URI and re-add
      val doc = place.doc
      doc.removeField(IndexFields.PLACE_SEED_URI)
      doc.add(new StringField(IndexFields.PLACE_SEED_URI, seedURI, Field.Store.YES))
      writer.addDocument(doc)
    })
  }

}