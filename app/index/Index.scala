package index

import index.objects._
import index.places._
import java.io.File
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.taxonomy.{ TaxonomyWriter, SearcherTaxonomyManager }
import org.apache.lucene.facet.taxonomy.directory.{ DirectoryTaxonomyReader, DirectoryTaxonomyWriter }
import org.apache.lucene.index.{ DirectoryReader, IndexWriter, IndexWriterConfig, MultiReader }
import org.apache.lucene.search.{ IndexSearcher, SearcherFactory }
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import play.api.Logger

private[index] class IndexBase(placeIndexDir: File, objectIndexDir: File, taxonomyDir: File) {
  
  private val placeIndex = FSDirectory.open(placeIndexDir)
  
  private val objectIndex = FSDirectory.open(objectIndexDir)
  
  private val taxonomyIndex = FSDirectory.open(taxonomyDir)
  
  protected var placeIndexReader = DirectoryReader.open(placeIndex)
  
  protected val analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT)

  protected val facetsConfig = new FacetsConfig()
  facetsConfig.setHierarchical(IndexFields.OBJECT_TYPE, false)

  protected val searcherTaxonomyMgr = new SearcherTaxonomyManager(objectIndex, taxonomyIndex, new SearcherFactory())
  
  protected def newObjectWriter(): (IndexWriter, TaxonomyWriter) =
    (new IndexWriter(objectIndex, new IndexWriterConfig(Version.LUCENE_CURRENT, analyzer)), new DirectoryTaxonomyWriter(taxonomyIndex))
    
  protected def newPlaceWriter(): IndexWriter = 
    new IndexWriter(placeIndex, new IndexWriterConfig(Version.LUCENE_CURRENT, analyzer))
  
  protected def newPlaceSearcher(): IndexSearcher = 
    new IndexSearcher(placeIndexReader)

  def numObjects: Int = {
    val searcherAndTaxonomy = searcherTaxonomyMgr.acquire()
    val numObjects = searcherAndTaxonomy.searcher.getIndexReader().numDocs()
    searcherTaxonomyMgr.release(searcherAndTaxonomy)
    numObjects
  }
  
  def numPlaceNetworks: Int =
    placeIndexReader.numDocs()
  
  def refresh() = {
    Logger.info("Refreshing index readers")
    searcherTaxonomyMgr.maybeRefresh()
    
    placeIndexReader.close()
    placeIndexReader = DirectoryReader.open(placeIndex)
  }
  
  def close() = {
    analyzer.close()
    searcherTaxonomyMgr.close()
    
    placeIndex.close()
    placeIndexReader.close()
    
    objectIndex.close()
    taxonomyIndex.close()
  }
      
}

class Index private(placeIndexDir: File, objectIndexDir: File, taxonomyDir: File)
  extends IndexBase(placeIndexDir, objectIndexDir, taxonomyDir)
    with ObjectReader
    with ObjectWriter
    with PlaceReader
    with PlaceWriter
  
object Index {
  
  def open(indexDir: String): Index = {
    val baseDir = new File(indexDir)
      
    val placeIndexDir = createIfNotExists(new File(baseDir, "gazetteer"))
    val objectIndexDir = createIfNotExists(new File(baseDir, "objects"))
    
    val taxonomyDirectory = new File(baseDir, "taxonomy")
    if (!taxonomyDirectory.exists) {
      taxonomyDirectory.mkdirs()
      val taxonomyInitializer = new DirectoryTaxonomyWriter(FSDirectory.open(taxonomyDirectory))
      taxonomyInitializer.close()
    }
    
    new Index(placeIndexDir, objectIndexDir, taxonomyDirectory)
  }
  
  private def createIfNotExists(dir: File): File = {
    if (!dir.exists) {
      dir.mkdirs()  
      val initConfig = new IndexWriterConfig(Version.LUCENE_CURRENT, new StandardAnalyzer(Version.LUCENE_CURRENT))
      val initializer = new IndexWriter(FSDirectory.open(dir), initConfig)
      initializer.close()      
    }
    
    dir  
  }
  
  def normalizeURI(uri: String) = {
    val noFragment = if (uri.indexOf('#') > -1) uri.substring(0, uri.indexOf('#')) else uri
    if (noFragment.endsWith("/"))
      noFragment.substring(0, noFragment.size - 1)
    else 
      noFragment
  }
  
}
