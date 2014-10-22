package index

import com.spatial4j.core.context.jts.JtsSpatialContext
import index.places.IndexedPlaceNetwork
import models.{ AggregatedView, AnnotatedThing, BoundingBox, Dataset }
import org.apache.lucene.document.{ Document, Field, StringField, TextField, IntField }
import org.apache.lucene.facet.FacetField
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree
import play.api.db.slick._
import com.vividsolutions.jts.geom.Envelope
import com.vividsolutions.jts.io.WKTWriter
import org.geotools.geometry.jts.JTS
import org.apache.lucene.document.StoredField

case class IndexedObject(private val doc: Document) {
 
  val objectType: IndexedObjectTypes.Value = { 
    val typeField = doc.get(IndexFields.OBJECT_TYPE)
    if (typeField != null) // The object type is defined through the value of the facet field    
      IndexedObjectTypes.withName(typeField)
    else // or it's a place network (in which case there is no facet field)
      IndexedObjectTypes.PLACE
  }
  
  val identifier: String =
    if (objectType == IndexedObjectTypes.PLACE) 
      doc.get(IndexFields.PLACE_URI) // The identifier is the first URI in the list for the place network
    else
      doc.get(IndexFields.ID) // or the ID for everything else
    
  val title: String = doc.get(IndexFields.TITLE)
    
  val description: Option[String] = Option(doc.get(IndexFields.DESCRIPTION))
  
  val temporalBoundsStart: Option[Int] = Option(doc.get(IndexFields.DATE_FROM)).map(_.toInt)
  
  val temporalBoundsEnd: Option[Int] = Option(doc.get(IndexFields.DATE_TO)).map(_.toInt)
  
  // TODO use typed bouding box
  val bbox = Option(doc.get(IndexFields.BBOX))
    .map(str => str.split(",").map(_.toDouble).toSeq)
    .map(c => (c(0), c(1), c(2), c(3)))
  
  def toPlaceNetwork = new IndexedPlaceNetwork(doc)
 
}

object IndexedObject {
	
  private val spatialCtx = JtsSpatialContext.GEO
  
  private val maxLevels = 11 //results in sub-meter precision for geohash
  
  private val spatialStrategy =
    new RecursivePrefixTreeStrategy(new GeohashPrefixTree(spatialCtx, maxLevels), IndexFields.GEOMETRY)
  
  def toDoc(thing: AnnotatedThing, datasetHierarchy: Seq[Dataset])(implicit s: Session): Document = {
    val doc = new Document()
    
    // ID, parent dataset ID, title, description, type = object
    doc.add(new StringField(IndexFields.ID, thing.id, Field.Store.YES))
    doc.add(new StringField(IndexFields.PUBLISHER, datasetHierarchy.head.publisher, Field.Store.NO))
    doc.add(new StringField(IndexFields.DATASET, thing.dataset, Field.Store.YES))
    doc.add(new TextField(IndexFields.TITLE, thing.title, Field.Store.YES))
    doc.add(new StringField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.ANNOTATED_THING.toString, Field.Store.YES))
    doc.add(new FacetField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.ANNOTATED_THING.toString))
    
    val datasetPath = datasetHierarchy(0).publisher +: datasetHierarchy.map(_.title)
    doc.add(new FacetField(IndexFields.DATASET, datasetPath:_*))
    
    // Temporal bounds
    thing.temporalBoundsStart.map(d => doc.add(new IntField(IndexFields.DATE_FROM, d, Field.Store.YES)))
    thing.temporalBoundsEnd.map(d => doc.add(new IntField(IndexFields.DATE_TO, d, Field.Store.YES)))
    
    // Place URIs
    val places = AggregatedView.findPlacesForThing(thing.id).items.map(_._1)
    places.foreach(gazetteerRef => doc.add(new StringField(IndexFields.PLACE_URI, Index.normalizeURI(gazetteerRef.uri), Field.Store.NO)))
 
    // Bounding box
    val geometries = places.filter(_.geometry.isDefined).map(_.geometry.get)
    if (geometries.size > 0) {
      val envelope = new Envelope()
      geometries.foreach(geom => envelope.expandToInclude(geom.getEnvelopeInternal))
      val bbox = 
        BoundingBox(envelope.getMinX, envelope.getMaxX, envelope.getMinY, envelope.getMaxY).toString
      doc.add(new StoredField(IndexFields.BBOX, bbox))
    }
    
    // Detailed geometry for spatial indexing
    geometries.foreach(geom => spatialStrategy.createIndexableFields(spatialCtx.makeShape(geom)).foreach(doc.add(_)))
    
    doc   
  }
  
  def toDoc(dataset: Dataset): Document = {
    val doc = new Document()
    doc.add(new StringField(IndexFields.ID, dataset.id, Field.Store.YES))
    doc.add(new TextField(IndexFields.TITLE, dataset.title, Field.Store.YES))
    dataset.description.map(d => doc.add(new TextField(IndexFields.DESCRIPTION, d, Field.Store.YES)))
    doc.add(new StringField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString, Field.Store.YES))
    dataset.temporalBoundsStart.map(d => doc.add(new IntField(IndexFields.DATE_FROM, d, Field.Store.YES)))
    dataset.temporalBoundsEnd.map(d => doc.add(new IntField(IndexFields.DATE_TO, d, Field.Store.YES)))
    doc.add(new FacetField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString))
    doc
  }
  
}

object IndexedObjectTypes extends Enumeration {
  
  val DATASET = Value("Dataset")
  
  val ANNOTATED_THING = Value("Item")
  
  val PLACE = Value("Place")

}

