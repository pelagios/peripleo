package index.places

import index.{ Index, IndexFields }
import org.apache.lucene.document.{ Field, StringField }
import org.apache.lucene.index.{ IndexWriter, Term }
import org.pelagios.api.gazetteer.Place
import play.api.Logger

trait PlaceWriter extends PlaceReader {
  
  def addPlaces(places: Iterable[Place]): Int =  { 
    val writer = newPlaceWriter()
    
    val distinctNewPlaces = places.map(place => {
      val normalizedUri = Index.normalizeURI(place.uri)
      
      // Enforce uniqueness
      if (findPlaceByURI(normalizedUri).isDefined) {
        Logger.warn("Place '" + place.uri + "' already in index!")
        0
      } else {
        // Places that this place lists as closeMatch
        val closeMatchesOut = place.closeMatches.map(uri => findPlaceByURI(Index.normalizeURI(uri))).filter(_.isDefined).map(_.get)

        // Places in the index that list this place as closeMatch
        val closeMatchesIn = findPlaceByCloseMatch(normalizedUri)
        
        val closeMatches = closeMatchesOut ++ closeMatchesIn
        
        // All closeMatches need to share the same seed URI
        val seedURI =
          if (closeMatches.size > 0) 
            closeMatches(0).seedURI
          else
            normalizedUri
 
        // Update seed URIs where necessary
        updateSeedURI(closeMatches.filter(!_.seedURI.equals(seedURI)), seedURI, writer)
        
        // Add new document to index
        val differentSeedURI = if (normalizedUri == seedURI) None else Some(seedURI)
        writer.addDocument(IndexedPlace.toDoc(place, Some(seedURI)))
        
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