package index

import org.apache.lucene.document.Document
import play.api.Logger

abstract class IndexedObject(private val doc: Document) {
 
  val objectType: IndexedObjectTypes.Value = { 
    val typeField = doc.get(IndexFields.OBJECT_TYPE)
    if (typeField != null)
      // The type is either defined through the index field
      IndexedObjectTypes.withName(typeField)
    else
      // or empty in case of places
      IndexedObjectTypes.PLACE
  }
  
  val identifier: String =
    if (objectType == IndexedObjectTypes.PLACE)
      doc.get(IndexFields.PLACE_URI)
    else
      doc.get(IndexFields.ID)
    
  val title: String = doc.get(IndexFields.TITLE)
    
  val description: Option[String] = Option(doc.get(IndexFields.DESCRIPTION))
 
}

