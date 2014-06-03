package global.index

import org.apache.lucene.index.{ IndexWriter, IndexWriterConfig }
import org.apache.lucene.util.Version
import models.{ AnnotatedThing, Dataset }
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter
import org.apache.lucene.facet.FacetsConfig

trait ObjectIndexWriter extends ObjectIndexBase {

  def addDataset(dataset: Dataset) = {
    val indexWriter = new IndexWriter(index, new IndexWriterConfig(Version.LUCENE_48, analyzer))
    val taxonomyWriter = new DirectoryTaxonomyWriter(taxonomy)
    indexWriter.addDocument(config.build(taxonomyWriter, IndexedObject(dataset)))
    indexWriter.close()
    taxonomyWriter.close()
  }
  
  def addAnnotatedThing(thing: AnnotatedThing) = addAnnotatedThings(Seq(thing))
  
  def addAnnotatedThings(things: Seq[AnnotatedThing]) = {
    val indexWriter = new IndexWriter(index, new IndexWriterConfig(Version.LUCENE_48, analyzer))
    val taxonomyWriter = new DirectoryTaxonomyWriter(taxonomy)
    things.foreach(t => indexWriter.addDocument(config.build(taxonomyWriter, IndexedObject(t))))
    indexWriter.close()
    taxonomyWriter.close()    
  }
  
}