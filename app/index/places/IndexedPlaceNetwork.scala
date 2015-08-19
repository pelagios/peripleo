package index.places

import com.spatial4j.core.context.jts.JtsSpatialContext
import com.vividsolutions.jts.geom.Geometry
import index.{ Index, IndexFields }
import index.objects.IndexedObjectTypes
import java.io.StringWriter
import models.geo.BoundingBox
import org.apache.lucene.document.{ Document, Field, NumericDocValuesField, StringField, StoredField, TextField }
import org.apache.lucene.facet.FacetField
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree
import org.geotools.geojson.geom.GeometryJSON
import play.api.libs.json.{ Json, JsObject }
import play.api.Logger
import java.util.UUID

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
  lazy val seedURI: String = doc.get(IndexFields.SEED_URI)
    
  lazy val names: Seq[String] = doc.getValues(IndexFields.PLACE_NAME).toSeq.distinct 
  
  lazy val languages: Seq[String] = doc.getValues(IndexFields.LANGUAGE).toSeq
  
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
  
  lazy val geometryJson: Option[String] = geometry.map(geom => {
    val geoJson = new StringWriter()
    new GeometryJSON().write(geom, geoJson)
    geoJson.toString
  })
    
  /** Network nodes and edges **/
  lazy val (nodes, edges) = {
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
  
  private val POINT = "Point"
  
  // TODO change policy:
  // 1. get all places with polygon geometry
  // One or more? Check priorities and pick, or just pick first one
  // None? Check priorities as before
  
  private def getPreferredGeometry(places: Seq[IndexedPlace]): Option[Geometry] = {

    // Returns the 1st place that comes from the highest-priority gazetteer, or the first list entry
    // if there's no place from a priority gazetteer
    def getPriorityPlace(priorityGazetteers: Seq[String], places: Seq[IndexedPlace]): Option[IndexedPlace] = {
      val topPriority = priorityGazetteers.foldLeft(Seq.empty[IndexedPlace])((result, gazetteer) => {
        if (result.isEmpty) {
          val firstPlaceForGazetteer = places.filter(_.sourceGazetteer.equalsIgnoreCase(gazetteer)).headOption
          Seq(firstPlaceForGazetteer).flatten
        } else {
          // We got a top priority geometry - no more checks needed
          result 
        }
      }).headOption  
      
      if (topPriority.isDefined)
        topPriority
      else
        places.headOption
    }

    val priorityGazetteers =  Seq("DARE", "iDAI", "Pleiades") // TODO needs to be made configurable through application.conf
      
    val placesWithGeometry = places.filter(_.geometry.isDefined)
    val placesWithPolygonGeometry = placesWithGeometry.filter(_.geometry.get.getGeometryType != POINT)

    val topPlaceWithPolygonGeom = getPriorityPlace(priorityGazetteers, placesWithPolygonGeometry)   
    if (topPlaceWithPolygonGeom.isDefined)
      topPlaceWithPolygonGeom.flatMap(_.geometry)
    else
      getPriorityPlace(priorityGazetteers, placesWithGeometry).flatMap(_.geometry)
  }

  def join(places: Seq[IndexedPlace], seedUri: Option[String] = None) = {
    val joinedDoc = new Document() 
    
    // If Seed URI isn't externally defined, we just use the URI of the first place
    seedUri match {
      case Some(uri) => joinedDoc.add(new StringField(IndexFields.SEED_URI, uri, Field.Store.YES))
      case None => joinedDoc.add(new StringField(IndexFields.SEED_URI, places.head.uri, Field.Store.YES))
    }
    
    joinedDoc.add(new StringField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.PLACE.toString, Field.Store.YES))
    joinedDoc.add(new FacetField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.PLACE.toString))
    joinedDoc.add(new NumericDocValuesField(IndexFields.BOOST, 4L)) // Places get boosted over other items
    
    places.foreach(addPlaceToDoc(_, joinedDoc))
    
    // Add seed URI as facet, so the place shows up in top places count as well (as if it were an object referencing one place)
    val seedURI = places.head.uri
    joinedDoc.add(new FacetField(IndexFields.PLACE_URI, Index.normalizeURI(seedURI)))
    
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
    join(network.places :+ place, Some(network.seedURI))
    
  /** Merges the place and the networks into one network **/
  def join(place: IndexedPlace, networks: Seq[IndexedPlaceNetwork]): IndexedPlaceNetwork = {
    val seedUri = if (networks.size > 0) networks.head.seedURI else place.uri
    join(networks.flatMap(_.places) :+ place, Some(seedUri))
  }
      
  private[places] def addPlaceToDoc(place: IndexedPlace, doc: Document): Document = {
    // Place URI
    doc.add(new StringField(IndexFields.ID, Index.normalizeURI(place.uri), Field.Store.YES))

    // Title
    val titleField = 
      if (doc.get(IndexFields.TITLE) == null)
        // If the network is still be empty, its title is null. In this case, store the place title as network title
        new TextField(IndexFields.TITLE, place.label, Field.Store.YES)
      else
        // Otherwise just index the place title, but don't store
        new TextField(IndexFields.TITLE, place.label, Field.Store.NO)  
    
    titleField.setBoost(1.5f) // Hits on title field should be weighted up slightly
    doc.add(titleField)
        
    // Description
    if (place.description.isDefined) {
      val descriptionField = if (doc.get(IndexFields.DESCRIPTION) == null)
        // If there is no stored description, store (and index) this one 
        new TextField(IndexFields.DESCRIPTION, place.description.get, Field.Store.YES)
      else
        // Otherwise, just index (but don't store)
        new TextField(IndexFields.DESCRIPTION, place.description.get, Field.Store.NO)
      
      descriptionField.setBoost(0.4f) // Hits on description field should be weighted down slightly
      doc.add(descriptionField)
    }
    
    // Index all names and languages (languages are additionally indexed as facet)
    place.names.map(_.chars).foreach(name => doc.add(new TextField(IndexFields.PLACE_NAME, name, Field.Store.YES)))
    
    val languages = place.names.flatMap(_.lang).distinct
    languages.foreach(lang => doc.add(new TextField(IndexFields.LANGUAGE, lang, Field.Store.YES)))
    languages.foreach(lang => doc.add(new FacetField(IndexFields.LANGUAGE, lang)))
    
    // Depictions
    place.depictions.foreach(depiction => doc.add(new StringField(IndexFields.DEPICTION, depiction, Field.Store.YES)))
    
    // Update list of source gazetteers, if necessary
    val sourceGazetteers = doc.getValues(IndexFields.SOURCE_DATASET).toSet
    if (!sourceGazetteers.contains(place.sourceGazetteer)) {
      doc.add(new TextField(IndexFields.SOURCE_DATASET, place.sourceGazetteer, Field.Store.YES))
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
      val dateRange = Index.dateRangeTree.parseShape("[" + start + " TO " + end + "]")
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
