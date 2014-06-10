package index.objects

import models.Page
import index.IndexBase
import index.IndexedObject

trait ObjectReader extends IndexBase {
  
  def search(query: String, offset: Int = 0, limit: Int = 20, fuzzy: Boolean = false): Page[IndexedObject] = { null }

}