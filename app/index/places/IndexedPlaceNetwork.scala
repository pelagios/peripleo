package index.places

import com.spatial4j.core.context.SpatialContext
import index.{ Index, IndexFields }
import org.apache.lucene.document.{ Document, Field, StringField, StoredField, TextField }
import org.pelagios.api.gazetteer.Place
import play.api.libs.json.{ Json, JsObject }
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree

case class NetworkNode(uri: String, place: Option[IndexedPlace])

case class NetworkEdge(source: Int, target: Int)

/** Represents a network of gazetteer records, interlinked with closeMatch statements.
  *  
  * A network is represented as a single document in the index. The 'sub'-places to the network
  * are not individually indexed (because we don't want them to show up as individual search 
  * results). Instead, they are stored as serialized JSON.  
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
class IndexedPlaceNetwork private[index] (private[index] val doc: Document) {
  
  /** The first place URI added to the network **/
  val seedURI: String = doc.get(IndexFields.PLACE_URI)

  /** The network title - identical to the title of the place that started the network **/
  val title: String = doc.get(IndexFields.TITLE)
  
  /** The network description - identical to the first place description added to the network **/
  val description: Option[String] = Option(doc.get(IndexFields.DESCRIPTION))
  
  /** All indexed places in this network **/
  val places: Seq[IndexedPlace] =
    doc.getValues(IndexFields.PLACE_AS_JSON).toSeq
      .map(new IndexedPlace(_))
  
  /** Network nodes and edges **/
  val (nodes, edges) = {
    val links = places.flatMap(p => Seq.fill(p.closeMatches.size)(p.uri).zip(p.closeMatches))   
    val nodes = (links.map(_._1) ++ links.map(_._2)).distinct.map(uri => NetworkNode(uri, places.find(_.uri == uri)))
    val edges = links.map { case (source, target) => 
      NetworkEdge(nodes.indexWhere(_.uri == source), nodes.indexWhere(_.uri == target)) }  
    (nodes, edges)
  }

  /** Helper method to get a place with a specific URI **/
  def getPlace(uri: String): Option[IndexedPlace] =
    places.find(_.uri == Index.normalizeURI(uri))
    
}

object IndexedPlaceNetwork {
  
  private val spatialCtx = SpatialContext.GEO
  
  private val maxLevels = 11 //results in sub-meter precision for geohash
  
  private val spatialStrategy =
    new RecursivePrefixTreeStrategy(new GeohashPrefixTree(spatialCtx, maxLevels), IndexFields.PLACE_GEOMETRY)
  
  /** Creates a new place network with a single place **/
  def createNew(): IndexedPlaceNetwork = 
    new IndexedPlaceNetwork(new Document())
  
  /** Merges the place into the network **/
  def join(place: IndexedPlace, network: IndexedPlaceNetwork): IndexedPlaceNetwork =
    join(place, Seq(network))
    
  /** Merges the place and the networks into one network **/
  def join(place: IndexedPlace, networks: Seq[IndexedPlaceNetwork]): IndexedPlaceNetwork = {
    val joinedDoc = new Document() 
    val allPlaces = networks.flatMap(_.places) :+ place
    allPlaces.foreach(addPlaceToDoc(_, joinedDoc))
    new IndexedPlaceNetwork(joinedDoc)   
  }
      
  private[places] def addPlaceToDoc(place: IndexedPlace, doc: Document): Document = {
    if (doc.get(IndexFields.TITLE) == null)
      // If the network is still be empty, its title is null. In this case, store the place title as network title
      doc.add(new TextField(IndexFields.TITLE, place.title, Field.Store.YES))
    else
      // Otherwise just index the place title, but don't store
      doc.add(new TextField(IndexFields.TITLE, place.title, Field.Store.NO))
      
    if (place.description.isDefined) {
      if (doc.get(IndexFields.DESCRIPTION) == null)
        // If there is no stored description, store (and index) this one 
        doc.add(new TextField(IndexFields.DESCRIPTION, place.description.get, Field.Store.YES))
      else
        // Otherwise, just index (but don't store)
        doc.add(new TextField(IndexFields.DESCRIPTION, place.description.get, Field.Store.NO)) 
    }
    
    // Index (but don't store) all names
    place.names.foreach(literal => doc.add(new StringField(IndexFields.PLACE_NAME, literal.chars, Field.Store.NO)))
    
    // Index & store place URI
    doc.add(new StringField(IndexFields.PLACE_URI, Index.normalizeURI(place.uri), Field.Store.YES))
    
    // Update list of source gazetteers, if necessary
    val sourceGazetteers = doc.getValues(IndexFields.PLACE_SOURCE_GAZETTEER).toSet
    if (!sourceGazetteers.contains(place.sourceGazetteer))
      doc.add(new StringField(IndexFields.PLACE_SOURCE_GAZETTEER, place.sourceGazetteer, Field.Store.YES))
      
    // Update list of close matches
    val newCloseMatches = place.closeMatches.map(Index.normalizeURI(_)).distinct
    val knownCloseMatches = doc.getValues(IndexFields.PLACE_CLOSE_MATCH).toSeq // These are distinct by definition
    newCloseMatches.diff(knownCloseMatches).foreach(closeMatch =>
      doc.add(new StringField(IndexFields.PLACE_CLOSE_MATCH, closeMatch, Field.Store.YES)))
      
    // Index shape geometry
    if (place.geometry.isDefined)
      spatialStrategy.createIndexableFields(spatialCtx.readShapeFromWkt(place.geometryWKT.get))
        .foreach(doc.add(_))
    
    // Add the JSON-serialized place as a stored (but not indexed) field
    doc.add(new StoredField(IndexFields.PLACE_AS_JSON, place.toString))    
    
    doc
  }
  
}