package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

/** AnnotatedThing model entity **/
case class AnnotatedThing(
  
  /** ID **/
  id: String, 
  
  /** ID of the dataset this thing is part of **/
  dataset: String, 
  
  /** dcterms:title **/
  title: String, 
   
  /** The ID of the annotated thing this thing is part of (if any) **/
  isPartOf: Option[String],

  /** foaf:homepage **/
  homepage: Option[String],
  
  /** The start of the date interval this thing is dated at (optional) **/ 
  temporalBoundsStart: Option[Int],
  
  /** The end of the date interval this thing is dated at (optional).
    *
    * If the thing is dated (i.e. if it has a temporalBoundsStart value)
    * this value MUST be set. In case the thing is dated with a datestamp
    * rather than an interval, temporalBoundsEnd must be the same as
    * temporalBoundsStart
    */   
  temporalBoundsEnd: Option[Int])

/** AnnotatedThing DB table **/
class AnnotatedThings(tag: Tag) extends Table[AnnotatedThing](tag, "annotated_things") {

  def id = column[String]("id", O.PrimaryKey)
  
  def datasetId = column[String]("dataset", O.NotNull)
  
  def title = column[String]("title", O.NotNull)
  
  def isPartOfId = column[String]("is_part_of", O.Nullable)
  
  def homepage = column[String]("homepage", O.Nullable)
  
  def temporalBoundsStart = column[Int]("temporal_bounds_start", O.Nullable)

  def temporalBoundsEnd = column[Int]("temporal_bounds_end", O.Nullable)

  def * = (id, datasetId, title, isPartOfId.?, homepage.?, temporalBoundsStart.?, 
    temporalBoundsEnd.?) <> (AnnotatedThing.tupled, AnnotatedThing.unapply)
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  def isPartOfFk = foreignKey("is_part_of_fk", isPartOfId, AnnotatedThings.query)(_.id)
  
  /** Indices **/
  
  def datasetIdx = index("idx_things_by_dataset", datasetId, unique = false)
  
}

/** Queries **/
object AnnotatedThings {
  
  private[models] val query = TableQuery[AnnotatedThings]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(thing: AnnotatedThing)(implicit s: Session) = query.insert(thing)
  
  def insertAll(things: Seq[AnnotatedThing])(implicit s: Session) = query.insertAll(things:_*)
  
  def update(thing: AnnotatedThing)(implicit s: Session) = 
    query.where(_.id === thing.id).update(thing)
  
  def countAll(topLevelOnly: Boolean = true)(implicit s: Session): Int = {
    if (topLevelOnly)
      Query(query.where(_.isPartOfId.isNull).length).first
    else
      Query(query.length).first
  }
    
  def listAll(topLevelOnly: Boolean = true, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[AnnotatedThing] = {
    val total = countAll(topLevelOnly)
    val result = 
      if (topLevelOnly)
        query.where(_.isPartOfId.isNull).drop(offset).take(limit).list
      else
        query.drop(offset).take(limit).list
        
    Page(result, offset, limit, total)
  }
        
  def findById(id: String)(implicit s: Session): Option[AnnotatedThing] = 
    query.where(_.id === id).firstOption

  def countByDataset(datasetId: String, topLevelOnly: Boolean = true)(implicit s: Session): Int =
    if (topLevelOnly)
      Query(query.where(_.datasetId === datasetId).where(_.isPartOfId.isNull).length).first
    else
      Query(query.where(_.datasetId === datasetId).length).first

  def findByDataset(datasetId: String, topLevelOnly: Boolean = true, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[AnnotatedThing] = {
    val total = countByDataset(datasetId, topLevelOnly)
    val result = 
      if (topLevelOnly)
        query.where(_.datasetId === datasetId).where(_.isPartOfId.isNull).drop(offset).take(limit).list
      else
        query.where(_.datasetId === datasetId).drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }
  
  def countChildren(parentId: String)(implicit s: Session): Int =
    Query(query.where(_.isPartOfId === parentId).length).first
    
  def listChildren(parentId: String, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[AnnotatedThing] = {
    val total = countChildren(parentId)
    val result = query.where(_.isPartOfId === parentId).drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }
    
  private[models] def walkChildren(parentId: String)(implicit s: Session): Seq[AnnotatedThing] = {
    val children = query.where(_.isPartOfId === parentId).list
    if (children.isEmpty)
      children
    else
      children.flatMap(thing => thing +: walkChildren(thing.id))    
  }
  
  def getParentHierarchy(thingId: String)(implicit s: Session): Seq[String] = {
    val parentId = query.where(_.id === thingId).where(_.isPartOfId.isNotNull).map(_.isPartOfId).firstOption
    if (parentId.isDefined) {
      parentId.get +: getParentHierarchy(parentId.get)
    } else {
      Seq.empty[String]
    }
  }
  
  def getParentIds(thingIds: Seq[String])(implicit s: Session): Seq[String] = {
    val parentIds = query.where(_.id.inSet(thingIds)).where(_.isPartOfId.isNotNull).map(_.isPartOfId).list.distinct
    if (parentIds.isEmpty) {
      parentIds
    } else {
      (parentIds ++ getParentIds(parentIds)).distinct
    }
  }
  
}
