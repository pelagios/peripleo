package models.geo

import java.sql.Date
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }
import models.ImportStatus

/** Gazetteer model entity **/
case class Gazetteer(
    
  name: String, 
  
  totalPlaces: Int,
  
  lastUpdate: Date,
  
  importStatus: ImportStatus.Value,
  
  importMessage: Option[String] = None
  
)

/** Gazetteer DB table **/
class Gazetteers(tag: SlickTag) extends Table[Gazetteer](tag, "gazetteers") {
  
  def name = column[String]("name", O.PrimaryKey)
  
  def totalPlaces = column[Int]("total_places", O.NotNull)
  
  def lastUpdate = column[Date]("last_update", O.NotNull)
  
  def importStatus = column[ImportStatus.Value]("import_status", O.NotNull)
  
  def importMessage = column[String]("import_message", O.Nullable)
  
  def * = (name, totalPlaces, lastUpdate, importStatus, importMessage.?) <> (Gazetteer.tupled, Gazetteer.unapply)
  
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
  
  def listAll(offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Seq[(Gazetteer, Seq[String])] = {
    val query = for {
      gazetteer <- queryGazetteers.drop(offset).take(limit)   
      prefix <- queryGazetteerPrefixes if gazetteer.name === prefix.gazetteer
    } yield (gazetteer, prefix)
    
    query.list.groupBy(_._1).mapValues(_.map(_._2.prefix)).toSeq  
  }
  
  def delete(name: String)(implicit s: Session)= {
    queryGazetteerPrefixes.where(_.gazetteer === name).delete
    queryGazetteers.where(_.name === name).delete
  } 
    
  def numTotalPlaces()(implicit s: Session): Int =
    queryGazetteers.map(_.totalPlaces).list.sum
   
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