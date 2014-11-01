package index

import com.spatial4j.core.context.jts.JtsSpatialContext
import index.places.IndexedPlaceNetwork
import models.{ AnnotatedThing, Dataset }
import org.apache.lucene.document.{ Document, Field, StringField, TextField, IntField }
import org.apache.lucene.facet.FacetField
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree
import play.api.db.slick._
import com.vividsolutions.jts.geom.Envelope
import com.vividsolutions.jts.io.WKTWriter
import org.geotools.geometry.jts.JTS
import org.apache.lucene.document.StoredField
import index.places.IndexedPlace
import models.ConvexHull

case class IndexedObject(private val doc: Document) {

  val objectType: IndexedObjectTypes.Value = IndexedObjectTypes.withName(doc.get(IndexFields.OBJECT_TYPE))

  val identifier: String =
    if (objectType == IndexedObjectTypes.PLACE) 
      doc.get(IndexFields.PLACE_URI) // The identifier is the first URI in the list for the place network
    else
      doc.get(IndexFields.ID) // or the ID for everything else   
      
  val title: String = doc.get(IndexFields.TITLE)
    
  val description: Option[String] = Option(doc.get(IndexFields.DESCRIPTION))
  
  val homepage: Option[String] = Option(doc.get(IndexFields.HOMEPAGE))
  
  val temporalBoundsStart: Option[Int] = Option(doc.get(IndexFields.DATE_FROM)).map(_.toInt)
  
  val temporalBoundsEnd: Option[Int] = Option(doc.get(IndexFields.DATE_TO)).map(_.toInt)
  
  val convexHull: Option[ConvexHull] = Option(doc.get(IndexFields.CONVEX_HULL)).map(ConvexHull.fromWKT(_))
      
  def toPlaceNetwork = new IndexedPlaceNetwork(doc)
 
}

object IndexedObject {
	
  private val spatialCtx = JtsSpatialContext.GEO
  
  private val maxLevels = 11 //results in sub-meter precision for geohash
  
  private val spatialStrategy =
    new RecursivePrefixTreeStrategy(new GeohashPrefixTree(spatialCtx, maxLevels), IndexFields.GEOMETRY)
  
  def toDoc(thing: AnnotatedThing, places: Seq[IndexedPlace], datasetHierarchy: Seq[Dataset]): Document = {
    val doc = new Document()
    
    // ID, publisher, parent dataset ID, title, description, homepage, type = AnnotatedThing
    doc.add(new StringField(IndexFields.ID, thing.id, Field.Store.YES))
    doc.add(new StringField(IndexFields.PUBLISHER, datasetHierarchy.head.publisher, Field.Store.NO))
    doc.add(new StringField(IndexFields.DATASET, thing.dataset, Field.Store.YES))
    doc.add(new TextField(IndexFields.TITLE, thing.title, Field.Store.YES))
    thing.description.map(description => new TextField(IndexFields.DESCRIPTION, description, Field.Store.YES))
    thing.homepage.map(homepage => doc.add(new StoredField(IndexFields.HOMEPAGE, homepage)))
    doc.add(new StringField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.ANNOTATED_THING.toString, Field.Store.YES))
    doc.add(new FacetField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.ANNOTATED_THING.toString))
    
    // Dataset hierarchy as facet
    doc.add(new FacetField(IndexFields.DATASET, datasetHierarchy.map(_.title):_*))
    
    // Temporal bounds
    thing.temporalBoundsStart.map(d => doc.add(new IntField(IndexFields.DATE_FROM, d, Field.Store.YES)))
    thing.temporalBoundsEnd.map(d => doc.add(new IntField(IndexFields.DATE_TO, d, Field.Store.YES)))

    // Convex hull
    thing.convexHull.map(cv => doc.add(new StoredField(IndexFields.CONVEX_HULL, cv.toString)))

    // Place URIs
    places.foreach(place => doc.add(new StringField(IndexFields.PLACE_URI, place.uri, Field.Store.NO))) 
    
    // Detailed geometry as spatially indexed features
    val geometries = places.filter(_.geometry.isDefined).map(_.geometry.get)
    geometries.foreach(geom => spatialStrategy.createIndexableFields(spatialCtx.makeShape(geom)).foreach(doc.add(_)))
    
    doc   
  }
  
  def toDoc(dataset: Dataset): Document = {
    val doc = new Document()
    
    // ID, publisher, parent dataset ID, title, description, homepage, type = Dataset
    doc.add(new StringField(IndexFields.ID, dataset.id, Field.Store.YES))
    doc.add(new StringField(IndexFields.PUBLISHER, dataset.publisher, Field.Store.NO))
    dataset.isPartOf.map(isPartOf => doc.add(new StoredField(IndexFields.IS_PART_OF, isPartOf)))
    doc.add(new TextField(IndexFields.TITLE, dataset.title, Field.Store.YES))
    dataset.description.map(d => doc.add(new TextField(IndexFields.DESCRIPTION, d, Field.Store.YES)))
    dataset.homepage.map(homepage => doc.add(new StoredField(IndexFields.HOMEPAGE, homepage)))
    doc.add(new StringField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString, Field.Store.YES))
    doc.add(new FacetField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString))
    
    // Temporal bounds    
    dataset.temporalBoundsStart.map(start => doc.add(new IntField(IndexFields.DATE_FROM, start, Field.Store.YES)))
    dataset.temporalBoundsEnd.map(end => doc.add(new IntField(IndexFields.DATE_TO, end, Field.Store.YES)))

    // Convex hull
    dataset.convexHull.map(cv => doc.add(new StoredField(IndexFields.CONVEX_HULL, cv.toString)))

    doc
  }
  
}

object IndexedObjectTypes extends Enumeration {
  
  val DATASET = Value("Dataset")
  
  val ANNOTATED_THING = Value("Item")
  
  val PLACE = Value("Place")

}

