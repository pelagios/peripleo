package index.places

import index.{ Index, IndexFields }
import org.apache.lucene.document.{ Field, StringField }
import org.apache.lucene.index.{ IndexWriter, Term }
import org.pelagios.api.gazetteer.Place
import play.api.Logger

trait PlaceWriter extends PlaceReader {
  
  def addPlaces(places: Iterable[Place]) =  { 
    val writer = newPlaceWriter()
    
    places.foreach(place => {
      val normalizedUri = Index.normalizeURI(place.uri)
      
      // Enforce uniqueness
      if (findPlaceByURI(normalizedUri).isDefined) {
        Logger.warn("Place '" + place.uri + "' already in index!")
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
      }   
    })
    
    writer.close()
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