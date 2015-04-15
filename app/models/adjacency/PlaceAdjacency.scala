package models.adjacency

import models.geo.GazetteerReference
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }
import models.geo.GazetteerReference
import models.geo.GazetteerReference
import models.core.AnnotatedThings
import play.api.Logger

case class PlaceAdjacency(
    
    /** Auto-inc ID **/
    id: Option[Int],
    
    /** Annotated thing ID **/
    annotatedThing: String,
    
    /** A place on the annotated thing **/
    place: GazetteerReference,
    
    /** The adjacent place **/
    nextPlace: GazetteerReference,
    
    /** The weight, i.e. no. of annotation adjacencies for this place adjacency **/
    weight: Int)
    
class PlaceAdjacencys(tag: SlickTag) extends Table[PlaceAdjacency](tag, "adjacency_places") {
  
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
  
  def annotatedThingId = column[String]("annotated_thing", O.NotNull)
  
  def placeURI = column[String]("place_uri", O.NotNull)
  
  def placeTitle = column[String]("place_title", O.NotNull)
  
  def placeLocation = column[String]("place_location", O.Nullable, O.DBType("text")) 
  
  def nextPlaceURI = column[String]("next_place_uri", O.NotNull)
  
  def nextPlaceTitle = column[String]("next_place_title", O.NotNull)
  
  def nextPlaceLocation = column[String]("next_place_location", O.Nullable, O.DBType("text"))
  
  def weight = column[Int]("weight", O.NotNull)
  
  def * = (id.?, annotatedThingId, (placeURI, placeTitle, placeLocation.?), (nextPlaceURI, nextPlaceTitle, nextPlaceLocation.?), weight).shaped <> (
    { case (id, annotatedThing, place, nextPlace, weight) => PlaceAdjacency(id, annotatedThing, GazetteerReference.tupled.apply(place), GazetteerReference.tupled.apply(nextPlace), weight) },
    { p: PlaceAdjacency => Some(p.id, p.annotatedThing, GazetteerReference.unapply(p.place).get, GazetteerReference.unapply(p.nextPlace).get, p.weight) })
  
  /** Indices **/
    
  def annotatedThingIdx = index("idx_annotated_thing", annotatedThingId, unique = false)
  
}

/** Queries **/
object PlaceAdjacencys {
  
  private[models] val query = TableQuery[PlaceAdjacencys]
  
  /** Creates the DB table **/
  def create()(implicit s: Session) = query.ddl.create
  
  /** Inserts a list of adjacency pairs into the DB **/
  def insertAll(adjacencies: Seq[PlaceAdjacency])(implicit s: Session) =
    query.insertAll(adjacencies:_*)
    
  /** Retrieves adjacency pairs for an annotated thing **/
  def findByAnnotatedThing(id: String)(implicit s: Session): PlaceAdjacencyGraph =
    new PlaceAdjacencyGraph(query.where(_.annotatedThingId === id).list)
    
  def findByAnnotatedThingRecursive(id: String)(implicit s: Session): PlaceAdjacencyGraph = {
    val allIds = id +: AnnotatedThings.listChildrenRecursive(id)
    
    val result = query.where(_.annotatedThingId inSet allIds)
      .groupBy(t => (t.placeURI, t.placeTitle, t.placeLocation, t.nextPlaceURI, t.nextPlaceTitle, t.nextPlaceLocation))
      .map(t => (t._1._1, t._1._2, t._1._3.?, t._1._4, t._1._5, t._1._6.?, t._2.map(_.weight).sum))
      .list
      .map { case (placeURI, placeTitle, placeGeom, nextPlaceURI, nextPlaceTitle, nextPlaceGeom, weight) => 
      PlaceAdjacency(None, id, GazetteerReference(placeURI, placeTitle, placeGeom), GazetteerReference(nextPlaceURI, nextPlaceTitle, nextPlaceGeom), weight.get) }
    
    new PlaceAdjacencyGraph(result)
  }
  
  /** Deletes adjacency pairs for a list of annotated things **/
  def deleteForAnnotatedThings(ids: Seq[String])(implicit s: Session) =
    query.where(_.annotatedThingId inSet ids).delete  
  
}