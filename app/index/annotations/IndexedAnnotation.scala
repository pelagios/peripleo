package index.annotations

// import com.vividsolutions.jts.geom.Geometry
import index.{ Index, IndexFields }
import index.objects.IndexedObjectTypes
import index.places.IndexedPlaceNetwork
import java.util.UUID
import models.core.{ Annotation, AnnotatedThing }
import org.apache.lucene.document.{ Document, Field, IntField, StringField, TextField }
import org.apache.lucene.facet.FacetField
import models.geo.BoundingBox

class IndexedAnnotation(private val doc: Document) {

  val uuid: UUID = UUID.fromString(doc.get(IndexFields.ID))   
      
  val dataset: String = doc.get(IndexFields.SOURCE_DATASET)
    
  val annotatedThing: String = doc.get(IndexFields.ANNOTATION_THING)
  
  val prefix: Option[String] = Option(doc.get(IndexFields.ANNOTATION_FULLTEXT_PREFIX))
  
  val quote: Option[String] = Option(doc.get(IndexFields.ANNOTATION_QUOTE))
  
  val suffix: Option[String] = Option(doc.get(IndexFields.ANNOTATION_FULLTEXT_SUFFIX))
  
  val text: String = Seq(
      Option(doc.get(IndexFields.ANNOTATION_FULLTEXT_PREFIX)),
      Option(doc.get(IndexFields.ANNOTATION_QUOTE)),
      Option(doc.get(IndexFields.ANNOTATION_FULLTEXT_SUFFIX))).flatten.mkString(" ")

}

object IndexedAnnotation {

  def toDoc(rootParent: AnnotatedThing, parent: AnnotatedThing, annotation: Annotation, place: IndexedPlaceNetwork,
      fulltextPrefix: Option[String], fulltextSuffix: Option[String]): Document = {
    
    val doc = new Document()
    
    // UUID, containing dataset & annotated thing
    doc.add(new StringField(IndexFields.ID, annotation.uuid.toString, Field.Store.YES))
    doc.add(new StringField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.ANNOTATION.toString, Field.Store.NO))
    doc.add(new StringField(IndexFields.SOURCE_DATASET, annotation.dataset, Field.Store.YES))
    doc.add(new StringField(IndexFields.ANNOTATION_THING, annotation.annotatedThing, Field.Store.YES))
    
    // Thing title and description
    doc.add(new TextField(IndexFields.TITLE, rootParent.title, Field.Store.YES))
    rootParent.description.map(description => new TextField(IndexFields.DESCRIPTION, description, Field.Store.YES))

    // Temporal bounds
    parent.temporalBoundsStart.map(start => doc.add(new IntField(IndexFields.DATE_FROM, start, Field.Store.YES)))
    parent.temporalBoundsEnd.map(end => doc.add(new IntField(IndexFields.DATE_TO, end, Field.Store.YES)))
    parent.temporalBoundsStart.map(start => {
      val end = parent.temporalBoundsEnd.getOrElse(start)
      val dateRange =
        if (start > end) // Minimal safety precaution... 
          Index.dateRangeTree.parseShape("[" + end + " TO " + start + "]")
        else
          Index.dateRangeTree.parseShape("[" + start + " TO " + end + "]")
          
      Index.temporalStrategy.createIndexableFields(dateRange).foreach(doc.add(_))
    })
    
    // Text
    annotation.quote.map(quote => doc.add(new TextField(IndexFields.ANNOTATION_QUOTE, quote, Field.Store.YES)))
    fulltextPrefix.map(text => doc.add(new TextField(IndexFields.ANNOTATION_FULLTEXT_PREFIX, text, Field.Store.YES)))
    fulltextSuffix.map(text => doc.add(new TextField(IndexFields.ANNOTATION_FULLTEXT_SUFFIX, text, Field.Store.YES)))
    
    // Place & geometry
    doc.add(new StringField(IndexFields.PLACE_URI, Index.normalizeURI(annotation.gazetteerURI), Field.Store.NO)) 
    doc.add(new FacetField(IndexFields.PLACE_URI, place.seedURI))
    
    // Bounding box to enable efficient best-fit queries
    val b = place.geometry.get.getEnvelopeInternal()
    Index.bboxStrategy.createIndexableFields(Index.spatialCtx.makeRectangle(b.getMinX, b.getMaxX, b.getMinY, b.getMaxY)).foreach(doc.add(_))
    
    doc
  }
  
}