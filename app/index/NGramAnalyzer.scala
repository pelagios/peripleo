package index

import java.io.{ Reader, StringReader }
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.core.{ LowerCaseFilter, StopAnalyzer, StopFilter }
import org.apache.lucene.analysis.shingle.ShingleFilter
import org.apache.lucene.analysis.standard.StandardTokenizer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.util.Version
import play.api.Logger
import scala.collection.mutable.ListBuffer

class NGramAnalyzer(size: Int) extends Analyzer {

  override def createComponents(fieldName: String, reader: Reader) = {
    val source = new StandardTokenizer(reader)

    val shingleFilter = new ShingleFilter(source, size)
    val lowerCaseFilter = new LowerCaseFilter(shingleFilter)
    val stopFilter = new StopFilter(lowerCaseFilter, StopAnalyzer.ENGLISH_STOP_WORDS_SET)
    
    new TokenStreamComponents(source, stopFilter)
  }
  
}

object NGramAnalyzer {
  
  private val CONTENTS = "contents"
  
  private val UNDERSCORE = "_"
  
  def tokenize(phrases: Set[String], size: Int = 3): Seq[String] = {
    val reader = new StringReader(phrases.mkString("\n"))
    
    val stream = new StopAnalyzer().tokenStream(CONTENTS, reader)
    val shingleFilter = new ShingleFilter(stream, size)
    val lowerCaseFilter = new LowerCaseFilter(shingleFilter)
    
    val charTermAttribute = lowerCaseFilter.getAttribute(classOf[CharTermAttribute])
    
    lowerCaseFilter.reset()
    val buffer = ListBuffer.empty[String]
    while(lowerCaseFilter.incrementToken) {
      // Remove Lucene's '_' stopword markers
      val token = charTermAttribute.toString.trim
      
      if (!token.contains(UNDERSCORE)) {
        // No stopword - just add
        buffer.append(token)
      } else if (token.startsWith(UNDERSCORE) || token.endsWith(UNDERSCORE)) {
        // Stopword on start or beginning - do nothing, the N-Gram without the stopword is already in the list
      } else {
        buffer.append(token.replace(UNDERSCORE, "").replace("  ", " "))
      }      
    }
      
    buffer.distinct.toSeq
  }
  
}