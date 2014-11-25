package models.geo

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }

/** Gazetteer model entity **/
case class Gazetteer(name: String, totalPlaces: Int)

/** Gazetteer DB table **/
class Gazetteers(tag: SlickTag) extends Table[Gazetteer](tag, "gazetteers") {
  
  def name = column[String]("name", O.PrimaryKey)
  
  def totalPlaces = column[Int]("total_places", O.NotNull)
  
  def * = (name, totalPlaces) <> (Gazetteer.tupled, Gazetteer.unapply)
  
}

/** Gazetteer URI prefix model entity **/
case class URIPrefix(id: Option[Int], gazetteer: String, prefix: String)

/** Gazetteer URI prefix DB table **/
class URIPrefixes(tag: SlickTag) extends Table[URIPrefix](tag, "gazetteer_uri_prefixes") {
  
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
  
  def gazetteer = column[String]("gazetteer", O.NotNull)
  
  def prefix = column[String]("uri_prefix", O.NotNull)
  
  def * = (id.?, gazetteer, prefix) <> (URIPrefix.tupled, URIPrefix.unapply)
  
  /** Foreign key constraints **/
  
  def gazetteerFk = foreignKey("gazetteer_name_fk", gazetteer, Gazetteers.queryGazetteers)(_.name)
  
}

/** Queries **/
object Gazetteers {
  
  private[models] val queryGazetteers = TableQuery[Gazetteers]
  
  private[models] val queryGazetteerPrefixes = TableQuery[URIPrefixes]
  
  def create()(implicit s: Session) = {
    queryGazetteers.ddl.create
    queryGazetteerPrefixes.ddl.create
  }
  
  def insert(gazetteer: Gazetteer, uriPrefixes: Seq[String])(implicit s: Session) = { 
    queryGazetteers.insert(gazetteer)
    
    val prefixes = uriPrefixes.map(URIPrefix(None, gazetteer.name, _))
    queryGazetteerPrefixes.insertAll(prefixes:_*)
  }
  
  def countAll()(implicit s: Session): Int =
    Query(queryGazetteers.length).first
  
  def listAll()(implicit s: Session): Seq[(Gazetteer, Seq[String])] = {
    val query = for {
      gazetteer <- queryGazetteers   
      prefix <- queryGazetteerPrefixes if gazetteer.name === prefix.gazetteer
    } yield (gazetteer, prefix)
    
    query.list.groupBy(_._1).mapValues(_.map(_._2.prefix)).toSeq  
  }
  
  def delete(name: String)(implicit s: Session)= {
    queryGazetteerPrefixes.where(_.gazetteer === name).delete
    queryGazetteers.where(_.name === name).delete
  } 
    
  def numTotalPlaces()(implicit s: Session): Int =
    queryGazetteers.map(_.totalPlaces).list.foldLeft(0)(_ + _)
   
  def findByName(name: String)(implicit s: Session): Option[Gazetteer] =
    queryGazetteers.where(_.name.toLowerCase === name.toLowerCase).firstOption
  
  def findByURI(uri: String)(implicit s: Session): Option[Gazetteer] = {
    val prefix = queryGazetteerPrefixes.list.find(p => uri.startsWith(p.prefix))
    if (prefix.isDefined)
      queryGazetteers.where(_.name === prefix.get.gazetteer).firstOption
    else
      None
  }
    
}