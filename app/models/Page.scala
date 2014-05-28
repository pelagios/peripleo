package models

/** A simple helper for wrapping paginated DB query results **/
case class Page[A](items: Seq[A], offset: Int, limit: Int, total: Long)

object Page {
  
  def empty[A] = Page(Seq.empty[A], 0, Int.MaxValue, 0)
  
}