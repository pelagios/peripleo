package global.index

import org.apache.lucene.index.{ IndexWriter, IndexWriterConfig }
import org.apache.lucene.util.Version
import models.{ AnnotatedThing, Dataset }

trait ObjectIndexWriter extends ObjectIndexBase {

  def addDataset(dataset: Dataset) = {
    val writer = new IndexWriter(index, new IndexWriterConfig(Version.LUCENE_47, analyzer))
    writer.addDocument(IndexedObject(dataset))
    writer.close()
  }
  
  def addAnnotatedThing(thing: AnnotatedThing) = {
    val writer = new IndexWriter(index, new IndexWriterConfig(Version.LUCENE_47, analyzer))
    writer.addDocument(IndexedObject(thing))
    writer.close()
  }
  
  def deleteDataset(id: String) = {
    
  }
  
  def deleteAnnotatedThing(id: String) = {
    
  }
  
}