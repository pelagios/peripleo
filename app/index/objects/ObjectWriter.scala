package index.objects

import models.{ Dataset, AnnotatedThing }
import org.apache.lucene.index.IndexWriterConfig
import index.IndexBase

trait ObjectWriter extends IndexBase {
  
    
  def addAnnotatedThing(annotatedThing: AnnotatedThing) = addAnnotatedThings(Seq(annotatedThing))
  
  def addAnnotatedThings(annotatedThings: Seq[AnnotatedThing]) = { 
    /*
    val indexWriter = new IndexWriter(index, new IndexWriterConfig(Version.LUCENE_48, analyzer))
    val taxonomyWriter = new DirectoryTaxonomyWriter(taxonomy)
    indexWriter.addDocument(config.build(taxonomyWriter, IndexedObject(dataset)))
    indexWriter.close()
    taxonomyWriter.close()
    */    
  }
  
  def addDataset(dataset: Dataset) = addDatasets(Seq(dataset))
  
  def addDatasets(datasets: Seq[Dataset]) = { }
  
  /*
  def addAnnotatedThing(annotatedThing: AnnotatedThing): Unit = addAnnotatedThings(Seq(annotatedThing))
  
  def addAnnotatedThings(annotatedThings: Seq[AnnotatedThing]): Unit = { }
  
  def addDataset(dataset: Dataset): Unit = addDatasets(Seq(dataset))
  
  def addDatasets(datasets: Seq[Dataset]): Unit = { }
   */

}