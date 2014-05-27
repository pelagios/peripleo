package global.index

import org.apache.lucene.index.{ IndexWriter, IndexWriterConfig }
import org.apache.lucene.util.Version
import models.{ AnnotatedThing, Dataset }
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter
import org.apache.lucene.facet.FacetsConfig

trait ObjectIndexWriter extends ObjectIndexBase {
  
  private final val config = new FacetsConfig();
  config.setHierarchical(ObjectIndex.FIELD_OBJECT_TYPE, false)

  def addDataset(dataset: Dataset) = {
    val indexWriter = new IndexWriter(index, new IndexWriterConfig(Version.LUCENE_48, analyzer))
    val taxonomyWriter = new DirectoryTaxonomyWriter(taxonomy)
    indexWriter.addDocument(config.build(taxonomyWriter, IndexedObject(dataset)))
    indexWriter.close()
    taxonomyWriter.close()
  }
  
  def addAnnotatedThing(thing: AnnotatedThing) = {
    val indexWriter = new IndexWriter(index, new IndexWriterConfig(Version.LUCENE_48, analyzer))
    val taxonomyWriter = new DirectoryTaxonomyWriter(taxonomy)
    indexWriter.addDocument(config.build(taxonomyWriter, IndexedObject(thing)))
    indexWriter.close()
    taxonomyWriter.close()
  }
  
}