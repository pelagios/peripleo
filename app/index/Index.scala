package index

import com.spatial4j.core.context.SpatialContext
import com.spatial4j.core.context.jts.JtsSpatialContext
import index.annotations._
import index.objects._
import index.places._
import index.suggest.SuggestIndex
import java.nio.file.{ Files, FileSystems, Path }
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.taxonomy.{ TaxonomyWriter, SearcherTaxonomyManager }
import org.apache.lucene.facet.taxonomy.directory.{ DirectoryTaxonomyReader, DirectoryTaxonomyWriter }
import org.apache.lucene.index.{ DirectoryReader, IndexWriter, IndexWriterConfig, MultiReader }
import org.apache.lucene.search.{ IndexSearcher, SearcherFactory, SearcherManager }
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.search.spell.{ SpellChecker, LuceneDictionary }
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree
import play.api.Logger
import org.apache.lucene.spatial.prefix.tree.DateRangePrefixTree
import org.apache.lucene.spatial.bbox.BBoxStrategy

private[index] class IndexBase(placeIndexDir: Path, objectIndexDir: Path, taxonomyDir: Path, annotationDir: Path, spellcheckDir: Path) {  
    
  /** Indices **/
  private val placeIndex = FSDirectory.open(placeIndexDir)
  
  private val objectIndex = FSDirectory.open(objectIndexDir)
  
  private val taxonomyIndex = FSDirectory.open(taxonomyDir)
  
  private val annotationIndex = FSDirectory.open(annotationDir)
  
  
  /** Index searcher managers **/  
  protected val placeSearcherManager = new SearcherTaxonomyManager(placeIndex, taxonomyIndex, new SearcherFactory())
  
  protected val objectSearcherManager = new SearcherTaxonomyManager(objectIndex, taxonomyIndex, new SearcherFactory())
  
  protected val annotationSearcherManager = new SearcherTaxonomyManager(annotationIndex, taxonomyIndex, new SearcherFactory())
  
  /** Analyzer **/
  protected val analyzer = new StandardAnalyzer() 
  
  
  /** Suggestion engine **/
  val suggester = new SuggestIndex(spellcheckDir, placeSearcherManager, objectSearcherManager, new NGramAnalyzer(3))  
  
  
  /** Index writers **/  
  protected lazy val placeWriter: IndexWriter = 
    new IndexWriter(placeIndex, new IndexWriterConfig(analyzer))
  
  protected lazy val objectWriter: IndexWriter =
    new IndexWriter(objectIndex, new IndexWriterConfig(analyzer))
  
  protected lazy val taxonomyWriter: TaxonomyWriter = 
    new DirectoryTaxonomyWriter(taxonomyIndex)
  
  protected lazy val annotationWriter: IndexWriter = 
    new IndexWriter(annotationIndex, new IndexWriterConfig(analyzer))
  
  
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
    
    taxonomyWriter.commit()
    placeWriter.commit()
    objectWriter.commit()
    annotationWriter.commit()
    
    placeSearcherManager.maybeRefresh()
    objectSearcherManager.maybeRefresh()
    annotationSearcherManager.maybeRefresh()
  }
  
  /** Closes all indices **/
  def close() = {
    analyzer.close()
    
    placeWriter.close()
    objectWriter.close()
    annotationWriter.close()
    taxonomyWriter.close()
    
    placeSearcherManager.close()
    objectSearcherManager.close()
    annotationSearcherManager.close()
    
    placeIndex.close()
    objectIndex.close()
    annotationIndex.close()
    taxonomyIndex.close()
    
    suggester.close()
  }
      
}

class Index private(placeIndexDir: Path, objectIndexDir: Path, taxonomyDir: Path, annotationDir: Path, spellcheckDir: Path)
  extends IndexBase(placeIndexDir, objectIndexDir, taxonomyDir, annotationDir, spellcheckDir)
    with ObjectReader
    with ObjectWriter
    with PlaceReader
    with PlaceWriter
    with AnnotationReader
    with AnnotationWriter
  
object Index {
  
  /** Spatial indexing settings **/
  private[index] val spatialCtx = JtsSpatialContext.GEO
  
  private[index] val maxSpatialTreeLevels = 11 
  
  private[index] val rptStrategy =
    new RecursivePrefixTreeStrategy(new GeohashPrefixTree(spatialCtx, maxSpatialTreeLevels), IndexFields.GEOMETRY)
  
  private[index] val bboxStrategy = 
    new BBoxStrategy(spatialCtx, IndexFields.BOUNDING_BOX)
  
  /** Time segment indexing settings **/
  private[index] val dateRangeTree = 
    DateRangePrefixTree.INSTANCE
  
  private[index] val temporalStrategy = 
    new NumberRangePrefixTreeStrategy(dateRangeTree, IndexFields.DATE_POINT);
  
  /** Object index facets **/
  private[index] val facetsConfig = new FacetsConfig()
  facetsConfig.setHierarchical(IndexFields.OBJECT_TYPE, false)
  facetsConfig.setHierarchical(IndexFields.SOURCE_DATASET, true)
  facetsConfig.setMultiValued(IndexFields.SOURCE_DATASET, true) 
  facetsConfig.setMultiValued(IndexFields.PLACE_URI, true)
  
  
  def open(indexDir: String): Index = {
    val fs = FileSystems.getDefault()
    val baseDir = fs.getPath(indexDir)
      
    val placeIndexDir = createIfNotExists(baseDir.resolve("gazetteer"))
    val objectIndexDir = createIfNotExists(baseDir.resolve("objects"))
    val annotationIndexDir = createIfNotExists(baseDir.resolve("annotations"))
    
    val taxonomyDirectory = baseDir.resolve("taxonomy")
    if (!Files.exists(taxonomyDirectory)) {
      Files.createDirectories(taxonomyDirectory)
      val taxonomyInitializer = new DirectoryTaxonomyWriter(FSDirectory.open(taxonomyDirectory))
      taxonomyInitializer.close()
    }
    
    val spellcheckIndexDir = createIfNotExists(baseDir.resolve("spellcheck"))
    
    new Index(placeIndexDir, objectIndexDir, taxonomyDirectory, annotationIndexDir, spellcheckIndexDir)
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
