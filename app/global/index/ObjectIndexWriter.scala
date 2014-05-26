package global.index

import org.apache.lucene.index.{ IndexWriter, IndexWriterConfig }
import org.apache.lucene.util.Version
import models.{ AnnotatedThing, Dataset }

trait ObjectIndexWriter extends ObjectIndexBase {

  def addDatasets(datasets: Iterable[Dataset]) = {
    val writer = new IndexWriter(index, new IndexWriterConfig(Version.LUCENE_47, analyzer))
    datasets.foreach(dataset => writer.addDocument(IndexedObject(dataset)))
    writer.close()
  }
  
  def addAnnotatedThing(thing: AnnotatedThing) = {
    val writer = new IndexWriter(index, new IndexWriterConfig(Version.LUCENE_47, analyzer))
    writer.addDocument(IndexedObject(thing))
    writer.close()
  }
  
}