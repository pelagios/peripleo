package index.suggest

import index.{ IndexFields, NGramAnalyzer }
import java.io.StringReader
import java.nio.file.Path
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager
import org.apache.lucene.index.{ DirectoryReader, IndexWriterConfig }
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.search.spell.{ LuceneDictionary, SpellChecker, PlainTextDictionary }
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester
import org.apache.lucene.store.FSDirectory
import play.api.Logger
import scala.collection.JavaConverters._

class SuggestIndex(directory: Path, placeSearcherManager: SearcherTaxonomyManager, objectSearcherManager: SearcherTaxonomyManager, analyzer: Analyzer) {
  
  protected val spellcheckIndex = FSDirectory.open(directory)
  
  protected val spellchecker = new SpellChecker(spellcheckIndex)
  
  lazy val suggester = {
    Logger.info("Initializing suggester")
    
    val reader = DirectoryReader.open(spellcheckIndex)  
    val dictionary = new LuceneDictionary(reader, SpellChecker.F_WORD)  
    
    val suggester = new AnalyzingSuggester(analyzer)
    // val suggester = new AnalyzingInfixSuggester(Version.LATEST, FSDirectory.open(new File(directory.getParent, "infix-suggester")), analyzer)
    
    // val suggester = new FuzzySuggester(analyzer, analyzer, 
    //  AnalyzingSuggester.EXACT_FIRST, 256, -1, true, 1, true, 
    //  3, FuzzySuggester.DEFAULT_MIN_FUZZY_LENGTH, false)
    
    suggester.build(dictionary)
    reader.close()

    Logger.info("Suggester initialized")
    suggester
  }

  /** (Re-)builds the spellcheck index **/
  def build() = {
    Logger.info("Building suggest index")
    
    spellchecker.clearIndex()
    
    val placeSearcher = placeSearcherManager.acquire()
    val objectSearcher = objectSearcherManager.acquire()
    
    val dictionarySources =
      // Relevant fields from the place index
      Seq(IndexFields.TITLE, IndexFields.PLACE_NAME, IndexFields.DESCRIPTION).map((_, placeSearcher.searcher.getIndexReader)) ++
      // Relevant fields from the object index
      Seq(IndexFields.TITLE, IndexFields.DESCRIPTION).map((_, objectSearcher.searcher.getIndexReader))
      
    try {
      dictionarySources.foreach { case (fieldName, reader) =>
        spellchecker.indexDictionary(new LuceneDictionary(reader, fieldName), new IndexWriterConfig(analyzer), true)
      }
    } finally {
      placeSearcherManager.release(placeSearcher)
      objectSearcherManager.release(objectSearcher)
    }
    
    Logger.info("Suggest index updated")
  }
  
  def addTerms(terms: Seq[String]) = {
    val nGrams = NGramAnalyzer.tokenize(terms)
    val dictionary = new PlainTextDictionary(new StringReader(nGrams.mkString("\n")))
    spellchecker.indexDictionary(dictionary, new IndexWriterConfig(analyzer), true)
  }
  
  def suggestCompletion(query: String, limit: Int): Seq[String] =
    suggester.lookup(query, false, limit).asScala.map(_.key.toString).sortBy(_.size)
  
  def suggestSimilar(query: String, limit: Int): Seq[String] =
    spellchecker.suggestSimilar(query, limit)
  
  def close() = {
    // suggester.close()
    spellchecker.close()
    spellcheckIndex.close()    
  }

}