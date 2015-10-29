package index.objects

import com.spatial4j.core.context.jts.JtsSpatialContext
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier
import index.Index
import index.IndexFields
import index.places.IndexedPlaceNetwork
import models.core.{ AnnotatedThing, Dataset, Image }
import models.geo.BoundingBox
import org.apache.lucene.document.{ Document, Field, NumericDocValuesField, StringField, StoredField, TextField, IntField }
import org.apache.lucene.facet.FacetField
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree
import play.api.db.slick._
import org.geotools.geojson.geom.GeometryJSON
import play.api.Logger
import org.geotools.geometry.jts.JTS

case class IndexedObject(private val doc: Document) {

  val objectType: IndexedObjectTypes.Value = IndexedObjectTypes.withName(doc.get(IndexFields.OBJECT_TYPE))

  val identifier: String = doc.get(IndexFields.ID)   
      
  val title: String = doc.get(IndexFields.TITLE)
    
  val description: Option[String] = Option(doc.get(IndexFields.DESCRIPTION))
  
  val datasetPath: Option[Seq[(String, String)]] = Option(doc.get(IndexFields.DATASET_HIERARCHY))
    .map(_.split(IndexedObject.DATASET_PATH_SEPARATOR).map(titleAndId => {
      val tuple = titleAndId.split(IndexedObject.DATASET_NAME_SEPARATOR)
      (tuple(0), tuple(1))
    }))
  
  val homepage: Option[String] = Option(doc.get(IndexFields.HOMEPAGE))
  
  val depictions: Seq[String] = doc.getValues(IndexFields.DEPICTION).toSeq
  
  val temporalBoundsStart: Option[Int] = Option(doc.get(IndexFields.DATE_FROM)).map(_.toInt)
  
  val temporalBoundsEnd: Option[Int] = Option(doc.get(IndexFields.DATE_TO)).map(_.toInt)
  
  // TODO optimize by storing the bounding box in the index?
  lazy val geoBounds: Option[BoundingBox] = geometry.map(geom => BoundingBox.fromGeometry(geom))
    
  lazy val geometry: Option[Geometry] = Option(doc.get(IndexFields.GEOMETRY)).map(geoJson => new GeometryJSON().read(geoJson.trim))
      
  def toPlaceNetwork = new IndexedPlaceNetwork(doc)
 
}

object IndexedObject {
  
  private val DATASET_PATH_SEPARATOR = Character.toString(7.asInstanceOf[Char]) // Beep character
  
  private val DATASET_NAME_SEPARATOR = "#"
  
  private val POLYGON_SIMPLIFICATION_TOLERANCE = 3.0
  
  def toDoc(thing: AnnotatedThing, places: Seq[(IndexedPlaceNetwork, String)], images: Seq[Image], fulltext: Option[String], datasetHierarchy: Seq[Dataset]): Document = {
    val doc = new Document()
    doc.add(new NumericDocValuesField(IndexFields.BOOST, 1L)) // Standard boost
    
    // ID, title, description, homepage
    doc.add(new StringField(IndexFields.ID, thing.id, Field.Store.YES))
    doc.add(new TextField(IndexFields.TITLE, thing.title, Field.Store.YES))
    thing.description.map(description => new TextField(IndexFields.DESCRIPTION, description, Field.Store.YES))
    thing.homepage.map(homepage => doc.add(new StoredField(IndexFields.HOMEPAGE, homepage)))
    
    // Object type = AnnotatedThing (stored + facet) 
    doc.add(new StringField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.ANNOTATED_THING.toString, Field.Store.YES))
    doc.add(new FacetField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.ANNOTATED_THING.toString))
    
    // Dataset hierarchy - all parent IDs for search
    datasetHierarchy.foreach(dataset => 
      doc.add(new StringField(IndexFields.SOURCE_DATASET, dataset.id, Field.Store.NO)))
    
    // Dataset hierarchy as stored & facet field (uses a custom {label}#{id} syntax to store human-readable title plus ID at the same time)
    val path = datasetHierarchy.map(d => d.title + DATASET_NAME_SEPARATOR + d.id).reverse // Note: hierarchy is from bottom leaf upwards, we want top-down
    doc.add(new StoredField(IndexFields.DATASET_HIERARCHY, path.mkString(DATASET_PATH_SEPARATOR)))
    doc.add(new FacetField(IndexFields.SOURCE_DATASET, path:_*))
    
    // Images
    images.foreach(image => doc.add(new StringField(IndexFields.DEPICTION, image.url, Field.Store.YES)))
    
    // Temporal bounds
    thing.temporalBoundsStart.map(start => doc.add(new IntField(IndexFields.DATE_FROM, start, Field.Store.YES)))
    thing.temporalBoundsEnd.map(end => doc.add(new IntField(IndexFields.DATE_TO, end, Field.Store.YES)))
    thing.temporalBoundsStart.map(start => {
      val end = thing.temporalBoundsEnd.getOrElse(start)
      val dateRange = Index.dateRangeTree.parseShape("[" + start + " TO " + end + "]")
      Index.temporalStrategy.createIndexableFields(dateRange).foreach(doc.add(_))
    })
    
    // Fulltext
    fulltext.map(text => doc.add(new TextField(IndexFields.ITEM_FULLTEXT, text, Field.Store.YES)))
    
    // Original place URIs from the annotations
    places.foreach { case (network, uri) => doc.add(new StringField(IndexFields.PLACE_URI, Index.normalizeURI(uri), Field.Store.NO)) }
    
    // Hull
    thing.hull.map(hull => {
      doc.add(new StoredField(IndexFields.GEOMETRY, hull.toString))
      
      // For indexing, we simplify the hull since this yields better performance
      val simplified = TopologyPreservingSimplifier.simplify(hull.geometry, POLYGON_SIMPLIFICATION_TOLERANCE)   
      Index.rptStrategy.createIndexableFields(Index.spatialCtx.makeShape(simplified)).foreach(doc.add(_))
    })
    
    // Bounding box to enable efficient best-fit queries
    thing.hull.map(_.bounds).map(b => 
      Index.bboxStrategy.createIndexableFields(Index.spatialCtx.makeRectangle(b.minLon, b.maxLon, b.minLat, b.maxLat))
        .foreach(doc.add(_)))
    
    doc   
  }
  
  def toDoc(dataset: Dataset): Document = {
    val doc = new Document()
    doc.add(new NumericDocValuesField(IndexFields.BOOST, 1L)) // Standard boost
    
    // ID, publisher, parent dataset ID, title, description, homepage, type = Dataset
    doc.add(new StringField(IndexFields.ID, dataset.id, Field.Store.YES))
    dataset.isPartOf.map(isPartOf => doc.add(new StoredField(IndexFields.IS_PART_OF, isPartOf)))
    doc.add(new TextField(IndexFields.TITLE, dataset.title, Field.Store.YES))
    dataset.description.map(d => doc.add(new TextField(IndexFields.DESCRIPTION, d, Field.Store.YES)))
    dataset.homepage.map(homepage => doc.add(new StoredField(IndexFields.HOMEPAGE, homepage)))
    doc.add(new StringField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString, Field.Store.YES))
    doc.add(new FacetField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString))
    
    // Temporal bounds    
    dataset.temporalBoundsStart.map(start => doc.add(new IntField(IndexFields.DATE_FROM, start, Field.Store.YES)))
    dataset.temporalBoundsEnd.map(end => doc.add(new IntField(IndexFields.DATE_TO, end, Field.Store.YES)))

    // Datasets just store the convex hull as geometry
    dataset.hull.map(cv => doc.add(new StoredField(IndexFields.GEOMETRY, cv.toString)))

    doc
  }
  
}

object IndexedObjectTypes extends Enumeration {
  
  val DATASET = Value("Dataset")
  
  val ANNOTATED_THING = Value("Item")
  
  val PLACE = Value("Place")

}

