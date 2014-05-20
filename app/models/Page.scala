package models

/** A simple helper for wrapping paginated DB query results **/
case class Page[A](items: Seq[A], offset: Int, limit: Int, total: Long)