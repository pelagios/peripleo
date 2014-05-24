package global.index

import java.io.File
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.search.SearcherFactory

private[index] class ObjectIndexBase(directory: File) {

  private val index = FSDirectory.open(directory)
  
  protected val searcherManager = new SearcherManager(index, new SearcherFactory())
  
  protected val analyzer = new StandardAnalyzer(Version.LUCENE_47)
  
  def close() = {
    analyzer.close()
    searcherManager.close()
    index.close()
  }
      
}

class ObjectIndex private(directory: File) extends ObjectIndexBase(directory) with ObjectIndexReader with ObjectIndexWriter

object ObjectIndex {
  
  val FIELD_ID = "id"
  
  val FIELD_TITLE = "title"
    
  val FIELD_DESCRIPTION = "description"
    
  val FIELD_OBJECT_TYPE = "type"
 
}
