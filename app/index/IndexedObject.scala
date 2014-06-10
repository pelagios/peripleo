package index

import models.{ AnnotatedThing, Dataset }
import org.apache.lucene.document.{ Document, Field, StringField, TextField }
import org.apache.lucene.facet.FacetField

case class IndexedObject(private val doc: Document) {
 
  val objectType: IndexedObjectTypes.Value = { 
    val typeField = doc.get(IndexFields.OBJECT_TYPE)
    if (typeField != null) // The object type is defined through the value of the facet field    
      IndexedObjectTypes.withName(typeField)
    else // or it's a place (in which case there is no facet field)
      IndexedObjectTypes.PLACE
  }
  
  val identifier: String =
    if (objectType == IndexedObjectTypes.PLACE) 
      doc.get(IndexFields.PLACE_URI) // The identifier is the URI for places
    else
      doc.get(IndexFields.ID) // or the ID for everything else
    
  val title: String = doc.get(IndexFields.TITLE)
    
  val description: Option[String] = Option(doc.get(IndexFields.DESCRIPTION))
 
}

object IndexedObject {
  
  def toDoc(thing: AnnotatedThing): Document = {
    val doc = new Document()
    doc.add(new StringField(IndexFields.ID, thing.id, Field.Store.YES))
    doc.add(new TextField(IndexFields.TITLE, thing.title, Field.Store.YES))
    doc.add(new FacetField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.ANNOTATED_THING.toString))
    doc   
  }
  
  def toDoc(dataset: Dataset): Document = {
    val doc = new Document()
    doc.add(new StringField(IndexFields.ID, dataset.id, Field.Store.YES))
    doc.add(new TextField(IndexFields.TITLE, dataset.title, Field.Store.YES))
    dataset.description.map(d => doc.add(new TextField(IndexFields.DESCRIPTION, d, Field.Store.YES)))
    doc.add(new FacetField(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString))
    doc
  }
  
}

object IndexedObjectTypes extends Enumeration {
  
  val DATASET = Value("Dataset")
  
  val ANNOTATED_THING = Value("Item")
  
  val PLACE = Value("Place")

}

