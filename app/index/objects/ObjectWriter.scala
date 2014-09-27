package index.objects

import models.{ Dataset, AnnotatedThing }
import org.apache.lucene.index.IndexWriterConfig
import index.IndexBase
import index.IndexedObject
import play.api.db.slick._

trait ObjectWriter extends IndexBase {
  
  def addAnnotatedThing(annotatedThing: AnnotatedThing)(implicit s: Session) =
    addAnnotatedThings(Seq(annotatedThing))
  
  def addAnnotatedThings(annotatedThings: Seq[AnnotatedThing])(implicit s: Session) = {
    val (indexWriter, taxonomyWriter) = newObjectWriter() 
    
    annotatedThings.foreach(thing =>
      indexWriter.addDocument(facetsConfig.build(taxonomyWriter, IndexedObject.toDoc(thing))))
    
    indexWriter.close()
    taxonomyWriter.close()   
  }
  
  def addDataset(dataset: Dataset) = addDatasets(Seq(dataset))
  
  def addDatasets(datasets: Seq[Dataset]) = { 
    val (indexWriter, taxonomyWriter) = newObjectWriter() 
    
    datasets.foreach(dataset =>
      indexWriter.addDocument(facetsConfig.build(taxonomyWriter, IndexedObject.toDoc(dataset))))
    
    indexWriter.close()
    taxonomyWriter.close()    
  }

}
