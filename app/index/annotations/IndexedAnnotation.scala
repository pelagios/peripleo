package index.annotations

import com.vividsolutions.jts.geom.Geometry
import index.{ Index, IndexFields }
import java.util.UUID
import models.core.Annotation
import org.apache.lucene.document.{ Document, Field, IntField, StringField, TextField }

class IndexedAnnotation(private val doc: Document) {

  val uuid: UUID = UUID.fromString(doc.get(IndexFields.ID))   
      
  val dataset: String = doc.get(IndexFields.ANNOTATION_DATASET)
    
  val annotatedThing: String = doc.get(IndexFields.ANNOTATION_THING)

}

object IndexedAnnotation {

  def toDoc(annotation: Annotation, temporalBoundsStart: Option[Int], temporalBoundsEnd: Option[Int], geometry: Geometry): Document = {
    val doc = new Document()
    
    // UUID, containing dataset & annotated thing
    doc.add(new StringField(IndexFields.ID, annotation.uuid.toString, Field.Store.YES))
    doc.add(new StringField(IndexFields.ANNOTATION_DATASET, annotation.dataset, Field.Store.YES))
    doc.add(new StringField(IndexFields.ANNOTATION_THING, annotation.annotatedThing, Field.Store.YES))
    
    // Temporal bounds
    temporalBoundsStart.map(d => doc.add(new IntField(IndexFields.DATE_FROM, d, Field.Store.NO)))
    temporalBoundsEnd.map(d => doc.add(new IntField(IndexFields.DATE_TO, d, Field.Store.NO)))
    
    // Quote
    annotation.quote.map(quote => doc.add(new TextField(IndexFields.ANNOTATION_QUOTE, quote, Field.Store.NO)))
    
    // TODO quote prefix and suffix
    
    // Place & geometry
    doc.add(new StringField(IndexFields.ANNOTATION_PLACE, annotation.gazetteerURI, Field.Store.NO)) 
    Index.spatialStrategy.createIndexableFields(Index.spatialCtx.makeShape(geometry)).foreach(doc.add(_))
        
    doc
  }
  
}