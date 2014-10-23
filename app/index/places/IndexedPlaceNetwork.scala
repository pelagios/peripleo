package index.places

import com.spatial4j.core.context.jts.JtsSpatialContext
import index.{ Index, IndexFields }
import org.apache.lucene.document.{ Document, Field, StringField, StoredField, TextField }
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree
import org.pelagios.api.gazetteer.Place
import play.api.libs.json.{ Json, JsObject }
import play.api.Logger
import com.vividsolutions.jts.geom.Envelope

case class NetworkNode(uri: String, place: Option[IndexedPlace], isInnerNode: Boolean)

case class NetworkEdge(source: Int, target: Int, isInnerEdge: Boolean)

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
    val links = places.flatMap(p => Seq.fill(p.matches.size)(p.uri).zip(p.matches))   
    val nodes = (links.map(_._1) ++ links.map(_._2)).distinct.map(uri => { 
      // If the node is an indexed place, it's an inner node; otherwise we require > 1 links to the node
      val place = places.find(_.uri == uri)
      val isInner = place.isDefined || links.filter(_._2 == uri).size > 1
      NetworkNode(uri, place, isInner) 
    })
    
    val edges = links.map { case (sourceURI, targetURI) => {
      val source = nodes.find(_.uri == sourceURI).get
      val target = nodes.find(_.uri == targetURI).get
      NetworkEdge(nodes.indexOf(source), nodes.indexOf(target), source.isInnerNode && target.isInnerNode) 
    }}  

    (nodes, edges)
  }

  /** Helper method to get a place with a specific URI **/
  def getPlace(uri: String): Option[IndexedPlace] =
    places.find(_.uri == Index.normalizeURI(uri))
    
  override def equals(o: Any) = o match {
    case other: IndexedPlaceNetwork => other.seedURI == seedURI
    case _  => false
  }
  
  override def hashCode = seedURI.hashCode
    
}

object IndexedPlaceNetwork {
  
  private val spatialCtx = JtsSpatialContext.GEO
  
  private val maxLevels = 9 // 11 results in sub-meter precision for geohash
  
  private val spatialStrategy =
    new RecursivePrefixTreeStrategy(new GeohashPrefixTree(spatialCtx, maxLevels), IndexFields.GEOMETRY)
  
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
    
    // Bounding box accross all place geometries
    val geometries = allPlaces.flatMap(_.geometry)
    if (geometries.size > 0) {
      val envelope = new Envelope() 
      geometries.foreach(geom => envelope.expandToInclude(geom.getEnvelopeInternal))
      val bbox = 
        Seq(envelope.getMinX, envelope.getMaxX, envelope.getMinY, envelope.getMaxY).mkString(",")
      joinedDoc.add(new StoredField(IndexFields.BBOX, bbox))
    }
    
    new IndexedPlaceNetwork(joinedDoc)   
  }
      
  private[places] def addPlaceToDoc(place: IndexedPlace, doc: Document): Document = {
    if (doc.get(IndexFields.TITLE) == null)
      // If the network is still be empty, its title is null. In this case, store the place title as network title
      doc.add(new TextField(IndexFields.TITLE, place.label, Field.Store.YES))
    else
      // Otherwise just index the place title, but don't store
      doc.add(new TextField(IndexFields.TITLE, place.label, Field.Store.NO))
      
    if (place.description.isDefined) {
      if (doc.get(IndexFields.DESCRIPTION) == null)
        // If there is no stored description, store (and index) this one 
        doc.add(new TextField(IndexFields.DESCRIPTION, place.description.get, Field.Store.YES))
      else
        // Otherwise, just index (but don't store)
        doc.add(new TextField(IndexFields.DESCRIPTION, place.description.get, Field.Store.NO)) 
    }
    
    // Index (but don't store) all names
    place.names.foreach(literal => doc.add(new TextField(IndexFields.PLACE_NAME, literal.chars, Field.Store.NO)))
    
    // Index & store place URI
    doc.add(new StringField(IndexFields.PLACE_URI, Index.normalizeURI(place.uri), Field.Store.YES))
    
    // Update list of source gazetteers, if necessary
    val sourceGazetteers = doc.getValues(IndexFields.PLACE_SOURCE_GAZETTEER).toSet
    if (!sourceGazetteers.contains(place.sourceGazetteer))
      doc.add(new StringField(IndexFields.PLACE_SOURCE_GAZETTEER, place.sourceGazetteer, Field.Store.YES))
      
    // Update list of matches
    val newMatches = place.matches.map(Index.normalizeURI(_)).distinct
    val knownMatches = doc.getValues(IndexFields.PLACE_MATCH).toSeq // These are distinct by definition
    newMatches.diff(knownMatches).foreach(anyMatch =>
      doc.add(new StringField(IndexFields.PLACE_MATCH, anyMatch, Field.Store.YES)))
          
    // Index shape geometry
    if (place.geometry.isDefined)
      try {
        spatialStrategy.createIndexableFields(spatialCtx.makeShape(place.geometry.get)).foreach(doc.add(_))
      } catch {
        case _: Throwable => Logger.info("Cannot index geometry: " + place.geometry.get)
      }
    
    // Add the JSON-serialized place as a stored (but not indexed) field
    doc.add(new StoredField(IndexFields.PLACE_AS_JSON, place.toString))    
    
    doc
  }
  
}
