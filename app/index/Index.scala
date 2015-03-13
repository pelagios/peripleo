package index

import index.objects._
import index.places._
import index.suggest.SuggestIndex
import java.nio.file.Path
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.taxonomy.{ TaxonomyWriter, SearcherTaxonomyManager }
import org.apache.lucene.facet.taxonomy.directory.{ DirectoryTaxonomyReader, DirectoryTaxonomyWriter }
import org.apache.lucene.index.{ DirectoryReader, IndexWriter, IndexWriterConfig, MultiReader }
import org.apache.lucene.search.{ IndexSearcher, SearcherFactory }
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.search.spell.{ SpellChecker, LuceneDictionary }
import play.api.Logger
import com.spatial4j.core.context.jts.JtsSpatialContext
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree
import java.nio.file.FileSystems
import java.nio.file.Files

private[index] class IndexBase(placeIndexDir: Path, objectIndexDir: Path, taxonomyDir: Path, spellcheckDir: Path) {  
    
  /** Indices **/
  private val placeIndex = FSDirectory.open(placeIndexDir)
  
  private val objectIndex = FSDirectory.open(objectIndexDir)
  
  private val taxonomyIndex = FSDirectory.open(taxonomyDir)
  
  
  /** Index searcher managers **/  
  protected val placeSearcherManager = new SearcherTaxonomyManager(placeIndex, taxonomyIndex, new SearcherFactory())
  
  protected val objectSearcherManager = new SearcherTaxonomyManager(objectIndex, taxonomyIndex, new SearcherFactory())
  
  
  /** We're using our own 3-word phrase analyzer **/
  protected val analyzer = new NGramAnalyzer(3)
  
  
  /** Suggestion engine **/
  val suggester = new SuggestIndex(spellcheckDir, placeSearcherManager, objectSearcherManager, analyzer)  
  
  
  /** Index writers **/
  protected lazy val taxonomyWriter: TaxonomyWriter = 
    new DirectoryTaxonomyWriter(taxonomyIndex)
  
  protected lazy val objectWriter: IndexWriter =
    new IndexWriter(objectIndex, new IndexWriterConfig(analyzer))
    
  protected lazy val placeWriter: IndexWriter = 
    new IndexWriter(placeIndex, new IndexWriterConfig(analyzer))

  
  /** Returns the number of objects in the object index **/
  def numObjects: Int = {
    val objectSearcher = objectSearcherManager.acquire()
    val numObjects = objectSearcher.searcher.getIndexReader().numDocs()
    objectSearcherManager.release(objectSearcher)
    numObjects
  }
  
  /** Returns the number of places in the gazetteer index **/
  def numPlaceNetworks: Int = {
    val searcherAndTaxonomy = placeSearcherManager.acquire()
    try {
      searcherAndTaxonomy.searcher.getIndexReader().numDocs()
    } finally {
      placeSearcherManager.release(searcherAndTaxonomy)
    }
  }
  
  /** Commits all writes and refreshes the readers **/
  def refresh() = {
    Logger.info("Committing index writes and refreshing readers")
    
    objectWriter.commit()
    placeWriter.commit()
    taxonomyWriter.commit()
    
    objectSearcherManager.maybeRefresh()
    placeSearcherManager.maybeRefresh()
  }
  
  /** Closes all indices **/
  def close() = {
    analyzer.close()
    
    objectWriter.close()
    placeWriter.close()
    taxonomyWriter.close()
    
    objectSearcherManager.close()
    placeSearcherManager.close()
    
    objectIndex.close()
    taxonomyIndex.close()
    placeIndex.close()
    
    suggester.close()
  }
      
}

class Index private(placeIndexDir: Path, objectIndexDir: Path, taxonomyDir: Path, spellcheckDir: Path)
  extends IndexBase(placeIndexDir, objectIndexDir, taxonomyDir, spellcheckDir)
    with ObjectReader
    with ObjectWriter
    with PlaceReader
    with PlaceWriter
  
object Index {
  
  /** Spatial indexing settings **/
  private[index] val spatialCtx = JtsSpatialContext.GEO
  
  private[index] val maxLevels = 11 
  
  private[index] val spatialStrategy =
    new RecursivePrefixTreeStrategy(new GeohashPrefixTree(spatialCtx, maxLevels), IndexFields.GEOMETRY)
  
  
  /** Object index facets **/
  private[index] val facetsConfig = new FacetsConfig()
  facetsConfig.setHierarchical(IndexFields.OBJECT_TYPE, false)
  facetsConfig.setHierarchical(IndexFields.ITEM_DATASET, true) 
  
  
  def open(indexDir: String): Index = {
    val fs = FileSystems.getDefault()
    val baseDir = fs.getPath(indexDir)
      
    val placeIndexDir = createIfNotExists(baseDir.resolve("gazetteer"))
    val objectIndexDir = createIfNotExists(baseDir.resolve("objects"))
    
    val taxonomyDirectory = baseDir.resolve("taxonomy")
    if (!Files.exists(taxonomyDirectory)) {
      Files.createDirectories(taxonomyDirectory)
      val taxonomyInitializer = new DirectoryTaxonomyWriter(FSDirectory.open(taxonomyDirectory))
      taxonomyInitializer.close()
    }
    
    val spellcheckIndexDir = createIfNotExists(baseDir.resolve("spellcheck"))
    
    new Index(placeIndexDir, objectIndexDir, taxonomyDirectory, spellcheckIndexDir)
  }
  
  private def createIfNotExists(dir: Path): Path = {
    if (!Files.exists(dir)) {
      Files.createDirectories(dir)
      val initConfig = new IndexWriterConfig(new StandardAnalyzer())
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
