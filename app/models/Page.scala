package models

case class Page[A](items: Seq[A], offset: Int, limit: Int, total: Long)