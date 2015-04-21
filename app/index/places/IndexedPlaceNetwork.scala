package index.places

import com.spatial4j.core.context.jts.JtsSpatialContext
import com.vividsolutions.jts.geom.Geometry
import index.{ Index, IndexFields }
import index.objects.IndexedObjectTypes
import java.io.StringWriter
import models.geo.BoundingBox
import org.apache.lucene.document.{ Document, Field, StringField, StoredField, TextField }
import org.apache.lucene.facet.FacetField
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree
import org.geotools.geojson.geom.GeometryJSON
import play.api.libs.json.{ Json, JsObject }
import play.api.Logger

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
  val seedURI: String = doc.get(IndexFields.ID)
  
  lazy val names: Seq[String] = doc.getValues(IndexFields.PLACE_NAME).toSeq.distinct 
  
  /** The other place URIs in the network **/ 
  lazy val alternativeURIs: Seq[String] = doc.getValues(IndexFields.ID).toSeq.diff(Seq(seedURI))

  /** The network title - identical to the title of the place that started the network **/
  val title: String = doc.get(IndexFields.TITLE)
  
  /** The network description - identical to the first place description added to the network **/
  val description: Option[String] = Option(doc.get(IndexFields.DESCRIPTION))
  
  /** All indexed places in this network **/
  val places: Seq[IndexedPlace] =
    doc.getValues(IndexFields.PLACE_AS_JSON).toSeq
      .map(new IndexedPlace(_))
      
  // TODO optimize by storing the bounding box in the index?
  lazy val geoBounds: Option[BoundingBox] = geometry.map(geom => BoundingBox.fromGeometry(geom))
  
  lazy val geometry: Option[Geometry] = Option(doc.get(IndexFields.GEOMETRY)).map(geoJson => new GeometryJSON().read(geoJson.trim))
  
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
  
  private def getPreferredGeometry(places: Seq[IndexedPlace]): Option[Geometry] = {
    val priorityList = Seq("DARE", "Pleiades") // TODO needs to be made configurable through application.conf
    
    val placesWithGeometry = places.filter(_.geometry.isDefined)
    
    val topPriority = priorityList.foldLeft(Seq.empty[IndexedPlace])((result, gazetteer) => {
      if (result.isEmpty) {
        val firstPlaceForGazetteer = placesWithGeometry.filter(_.sourceGazetteer.equalsIgnoreCase(gazetteer)).headOption
        Seq(firstPlaceForGazetteer).flatten
      } else {
        // We got a top priority geometry - no more checks needed
        result 
      }
    }).headOption
    
    if (topPriority.isDefined)
      // We got a priority geometry!
      topPriority.flatMap(_.geometry)
    else
      // No priority geom - just use first available
      placesWithGeometry.headOption.flatMap(_.geometry)
  }
  
  /** Creates a new place network with a single place **/
  def createNew(): IndexedPlaceNetwork = 
    new IndexedPlaceNetwork(new Document())
  
  def join(places: Seq[IndexedPlace]) = {
    val joinedDoc = new Document() 
    joinedDoc.add(new StringField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.PLACE.toString, Field.Store.YES))
    joinedDoc.add(new FacetField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.PLACE.toString))
    
    places.foreach(addPlaceToDoc(_, joinedDoc))
    
    getPreferredGeometry(places).map(geometry => {
      try {
        // Bounding box to enable efficient best-fit queries
        val bbox = BoundingBox.fromGeometry(geometry)
        Index.bboxStrategy.createIndexableFields(Index.spatialCtx.makeRectangle(bbox.minLon, bbox.maxLon, bbox.minLat, bbox.maxLat))
          .foreach(joinedDoc.add(_))
    
        val geoJson = new StringWriter()
        new GeometryJSON().write(geometry, geoJson)
        joinedDoc.add(new StoredField(IndexFields.GEOMETRY, geoJson.toString))
      } catch {
        case _: Throwable => Logger.info("Cannot index geometry: " + geometry)
      }
    })

    new IndexedPlaceNetwork(joinedDoc)
  }
  
  /** Merges the place into the network **/
  def join(place: IndexedPlace, network: IndexedPlaceNetwork): IndexedPlaceNetwork =
    join(network.places :+ place)
    
  /** Merges the place and the networks into one network **/
  def join(place: IndexedPlace, networks: Seq[IndexedPlaceNetwork]): IndexedPlaceNetwork =
    join(networks.flatMap(_.places) :+ place)
      
  private[places] def addPlaceToDoc(place: IndexedPlace, doc: Document): Document = {
    // Place URI
    doc.add(new StringField(IndexFields.ID, Index.normalizeURI(place.uri), Field.Store.YES))

    // Title
    if (doc.get(IndexFields.TITLE) == null)
      // If the network is still be empty, its title is null. In this case, store the place title as network title
      doc.add(new TextField(IndexFields.TITLE, place.label, Field.Store.YES))
    else
      // Otherwise just index the place title, but don't store
      doc.add(new TextField(IndexFields.TITLE, place.label, Field.Store.NO))
      
    // Description
    if (place.description.isDefined) {
      if (doc.get(IndexFields.DESCRIPTION) == null)
        // If there is no stored description, store (and index) this one 
        doc.add(new TextField(IndexFields.DESCRIPTION, place.description.get, Field.Store.YES))
      else
        // Otherwise, just index (but don't store)
        doc.add(new TextField(IndexFields.DESCRIPTION, place.description.get, Field.Store.NO)) 
    }
    
    // Index all names
    place.names.foreach(literal => doc.add(new TextField(IndexFields.PLACE_NAME, literal.chars, Field.Store.YES)))
    
    // Update list of source gazetteers, if necessary
    val sourceGazetteers = doc.getValues(IndexFields.SOURCE_DATASET).toSet
    if (!sourceGazetteers.contains(place.sourceGazetteer)) {
      doc.add(new StringField(IndexFields.SOURCE_DATASET, place.sourceGazetteer, Field.Store.YES))
      doc.add(new FacetField(IndexFields.SOURCE_DATASET, "gazetteer:" + place.sourceGazetteer))
    }
      
    // Update list of matches
    val newMatches = place.matches.map(Index.normalizeURI(_)).distinct
    val knownMatches = doc.getValues(IndexFields.PLACE_MATCH).toSeq // These are distinct by definition
    newMatches.diff(knownMatches).foreach(anyMatch =>
      doc.add(new StringField(IndexFields.PLACE_MATCH, anyMatch, Field.Store.YES)))
      
    // Temporal bounds
    place.temporalBoundsStart.map(start => {
      val end = place.temporalBoundsEnd.getOrElse(start)
      val dateRange =
        if (start > end) // Minimal safety precaution... 
          Index.dateRangeTree.parseShape("[" + end + " TO " + start + "]")
        else
          Index.dateRangeTree.parseShape("[" + start + " TO " + end + "]")
          
      Index.temporalStrategy.createIndexableFields(dateRange).foreach(doc.add(_))
    })
      
    // Index shape geometry
    if (place.geometry.isDefined)
      try {
        Index.rptStrategy.createIndexableFields(Index.spatialCtx.makeShape(place.geometry.get)).foreach(doc.add(_))
      } catch {
        case _: Throwable => Logger.info("Cannot index geometry: " + place.geometry.get)
      }
    
    // Add the JSON-serialized place as a stored (but not indexed) field
    doc.add(new StoredField(IndexFields.PLACE_AS_JSON, place.toString))    
    
    doc
  }
  
  def buildNetworks(places: Seq[IndexedPlace]): Seq[IndexedPlaceNetwork] = {
    
    def isConnected(place: IndexedPlace, network: Seq[IndexedPlace]): Boolean = {
      val networkURIs = network.map(_.uri)
      val networkMatches = network.flatMap(_.matches)
      
      val outboundMatches = place.matches.exists(skosMatch => networkURIs.contains(skosMatch))
      val inboundMatches = networkMatches.contains(place.uri)
      val indirectMatches = place.matches.exists(skosMatch => networkMatches.contains(skosMatch))

      outboundMatches || inboundMatches || indirectMatches      
    }
    
    val networks = places.foldLeft(Seq.empty[Seq[IndexedPlace]])((networks, place) => {  
      val connectedNetworks = networks.filter(network => isConnected(place, network))
      val disconnectedNetworks = networks.diff(connectedNetworks)
      
      if (connectedNetworks.size == 0) {
        // Not connected with anything - add as a new network
        networks :+ Seq(place)
      } else {
        // Connected with one or more networks - flatten and append
        disconnectedNetworks :+ (connectedNetworks.flatten :+ place)
      }
    })
    
    networks.map(join(_))
  }
  
}
