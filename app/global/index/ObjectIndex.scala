package global.index

import java.io.File
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.search.SearcherFactory
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter

private[index] class ObjectIndexBase(indexDir: File, taxonomyDir: File) {

  protected val index = FSDirectory.open(indexDir)
  
  protected val taxonomy = FSDirectory.open(taxonomyDir)
  
  protected val searcherManager = new SearcherManager(index, new SearcherFactory())
  
  protected val analyzer = new StandardAnalyzer(Version.LUCENE_47)
  
  def close() = {
    analyzer.close()
    searcherManager.close()
    index.close()
  }
      
}

class ObjectIndex private(indexDir: File, taxonomyDir: File) extends ObjectIndexBase(indexDir, taxonomyDir) with ObjectIndexReader with ObjectIndexWriter

object ObjectIndex {
  
  val FIELD_ID = "id"
  
  val FIELD_TITLE = "title"
    
  val FIELD_DESCRIPTION = "description"
    
  val FIELD_OBJECT_TYPE= "Type"
    
  val CATEGORY_DATASET = "Dataset"
    
  val CATEGORY_ANNOTATED_THING = "Item"
    
  def open(indexDir: String, taxonomyDir: String): ObjectIndex = {
    val indexDirectory = createIfNotExists(indexDir)
    val idxInitializer = new IndexWriter(FSDirectory.open(indexDirectory), 
      new IndexWriterConfig(Version.LUCENE_47, new StandardAnalyzer(Version.LUCENE_47)))
    idxInitializer.close()
    
    val taxonomyDirectory = createIfNotExists(indexDir)
    val taxonomyInitializer = new DirectoryTaxonomyWriter(FSDirectory.open(taxonomyDirectory))
    taxonomyInitializer.close()
    
    new ObjectIndex(indexDirectory, taxonomyDirectory)
  }
  
  private def createIfNotExists(dir: String): File = {
    val directory = new File(dir)
    if (!directory.exists)
      directory.mkdirs()
      
    directory
  }
 
}
