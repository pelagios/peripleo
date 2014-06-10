package index.objects

import models.{ Dataset, AnnotatedThing }
import org.apache.lucene.index.IndexWriterConfig
import index.IndexBase
import index.IndexedObject

trait ObjectWriter extends IndexBase {
  
  def addAnnotatedThing(annotatedThing: AnnotatedThing) = addAnnotatedThings(Seq(annotatedThing))
  
  def addAnnotatedThings(annotatedThings: Seq[AnnotatedThing]) = {
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