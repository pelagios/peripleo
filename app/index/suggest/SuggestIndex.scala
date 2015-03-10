package index.suggest

import index.IndexFields
import java.io.{ File, StringReader }
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.search.spell.{ LuceneDictionary, SpellChecker }
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import play.api.Logger
import org.apache.lucene.search.suggest.FileDictionary
import org.apache.lucene.search.spell.PlainTextDictionary
import index.NGramAnalyzer

class SuggestIndex(directory: File, placeSearcherManager: SearcherManager, objectSearcherManager: SearcherTaxonomyManager, analyzer: Analyzer) {
  
  protected val spellcheckIndex = FSDirectory.open(directory)
  
  protected val spellchecker = new SpellChecker(spellcheckIndex)

  /** (Re-)builds the spellcheck index **/
  def build() = {
    Logger.info("Building suggest index")
    
    spellchecker.clearIndex()
    
    val placeSearcher = placeSearcherManager.acquire()
    val objectSearcher = objectSearcherManager.acquire()
    
    val dictionarySources =
      // Relevant fields from the place index
      Seq(IndexFields.TITLE, IndexFields.PLACE_NAME, IndexFields.DESCRIPTION).map((_, placeSearcher.getIndexReader)) ++
      // Relevant fields from the object index
      Seq(IndexFields.TITLE, IndexFields.DESCRIPTION).map((_, objectSearcher.searcher.getIndexReader))
      
    try {
      dictionarySources.foreach { case (fieldName, reader) =>
        spellchecker.indexDictionary(new LuceneDictionary(reader, fieldName), new IndexWriterConfig(Version.LATEST, analyzer), true)
      }
    } finally {
      placeSearcherManager.release(placeSearcher)
      objectSearcherManager.release(objectSearcher)
    }
    
    Logger.info("Suggest index updated")
  }
  
  def addTerms(terms: Set[String]) = {
    val nGrams = NGramAnalyzer.tokenize(terms)
    val dictionary = new PlainTextDictionary(new StringReader(nGrams.mkString("\n")))
    spellchecker.indexDictionary(dictionary, new IndexWriterConfig(Version.LATEST, analyzer), true)
  }
  
  def suggestSimilar(query: String, limit: Int): Seq[String] =
    spellchecker.suggestSimilar(query, limit)
  
  def close() = {
    spellchecker.close()
    spellcheckIndex.close()    
  }

}