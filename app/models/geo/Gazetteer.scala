package models.geo

import java.sql.Timestamp
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }
import models.ImportStatus
import play.api.Logger

/** Gazetteer model entity **/
case class Gazetteer(
    
  name: String, 
  
  totalPlaces: Long,
  
  lastUpdate: Timestamp,
  
  importStatus: ImportStatus.Value,
  
  importProgress: Option[Double],
  
  importMessage: Option[String]
  
)

/** Gazetteer DB table **/
class Gazetteers(tag: SlickTag) extends Table[Gazetteer](tag, "gazetteers") {
  
  def name = column[String]("name", O.PrimaryKey)
  
  def totalPlaces = column[Long]("total_places", O.NotNull)
  
  def lastUpdate = column[Timestamp]("last_update", O.NotNull)
  
  def importStatus = column[ImportStatus.Value]("import_status", O.NotNull)
  
  def importProgress = column[Double]("import_progress", O.Nullable)
  
  def importMessage = column[String]("import_message", O.Nullable)
  
  def * = (name, totalPlaces, lastUpdate, importStatus, importProgress.?, 
    importMessage.?) <> (Gazetteer.tupled, Gazetteer.unapply)
  
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
  
  def insert(gazetteer: Gazetteer)(implicit s: Session) =
    queryGazetteers.insert(gazetteer)
  
    
  def setURIPrefixes(gazetteerName: String, prefixes: Seq[String])(implicit s: Session) = {
    val uriPrefixes = prefixes.map(URIPrefix(None, gazetteerName, _))
    queryGazetteerPrefixes.insertAll(uriPrefixes:_*)
  }
  
  def setLastUpdate(gazetteerName: String, date: Timestamp)(implicit s: Session) = 
    queryGazetteers.where(_.name === gazetteerName).map(_.lastUpdate).update(date)
  
  def setImportStatus(gazetteerName: String, status: ImportStatus.Value,
      progress: Option[Double] = None, message: Option[String] = None,
      totalPlaces: Option[Long] = None)(implicit s: Session) = {
    
    val q = queryGazetteers.where(_.name === gazetteerName)
    if (totalPlaces.isDefined) {
      q.map(g => (g.totalPlaces, g.importStatus, g.importProgress.?, g.importMessage.?))
       .update((totalPlaces.get, status, progress, message))
    } else {
      q.map(g => (g.importStatus, g.importProgress.?, g.importMessage.?))
      .update((status, progress, message))
    }
  }
    
  def countAll()(implicit s: Session): Int =
    Query(queryGazetteers.length).first
  
  def listAll(offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Seq[(Gazetteer, Seq[String])] = {
    val query = for {
      (g, p) <- queryGazetteers.drop(offset).take(limit) leftJoin queryGazetteerPrefixes on (_.name === _.gazetteer)   
    } yield (g, p.prefix.?)
    
    query.list.groupBy(_._1).mapValues(_.flatMap(_._2)).toSeq  
  }
  
  def delete(name: String)(implicit s: Session)= {
    queryGazetteerPrefixes.where(_.gazetteer === name).delete
    queryGazetteers.where(_.name === name).delete
  } 
    
  def numTotalPlaces()(implicit s: Session): Long =
    queryGazetteers.map(_.totalPlaces).list.sum
   
  def findByName(name: String)(implicit s: Session): Option[Gazetteer] =
    queryGazetteers.where(_.name.toLowerCase === name.toLowerCase).firstOption
    
  def findByNameWithPrefixes(name: String)(implicit s: Session): Option[(Gazetteer, Seq[String])] = {
    val query = for {
      (g, p) <- queryGazetteers where (_.name.toLowerCase === name.toLowerCase) leftJoin queryGazetteerPrefixes on (_.name === _.gazetteer)   
    } yield (g, p.prefix.?)

    val result = query.list
    if (result.size > 0)
      Some(result.head._1, result.flatMap(_._2))
    else
      None
  }
  
  def findByURI(uri: String)(implicit s: Session): Option[Gazetteer] = {
    val prefix = queryGazetteerPrefixes.list.find(p => uri.startsWith(p.prefix))
    if (prefix.isDefined)
      queryGazetteers.where(_.name === prefix.get.gazetteer).firstOption
    else
      None
  }
    
}