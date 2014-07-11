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
  
  def isPartOfFk = foreignKey("is_part_of_thing_fk", isPartOfId, AnnotatedThings.query)(_.id)
  
  /** Indices **/
  
  def datasetIdx = index("idx_things_by_dataset", datasetId, unique = false)
  
}

/** Queries **/
object AnnotatedThings {
  
  private[models] val query = TableQuery[AnnotatedThings]
  
  /** Creates the DB table **/
  def create()(implicit s: Session) = query.ddl.create
  
  /** Inserts a single AnnotatedThing into the DB **/
  def insert(thing: AnnotatedThing)(implicit s: Session) = query.insert(thing)

  /** Inserts a list of AnnotatedThings into the DB **/
  def insertAll(things: Seq[AnnotatedThing])(implicit s: Session) = query.insertAll(things:_*)
  
  /** Updates an AnnotatedThing **/
  def update(thing: AnnotatedThing)(implicit s: Session) = 
    query.where(_.id === thing.id).update(thing)
    
  /** Deletes an AnnotatedThing **/
  def delete(id: String)(implicit s: Session) =
    query.where(_.id === id).delete

  /** Counts all AnnotatedThings in the DB.
    * 
    * @param topLevelOnly if set to true, only top-level things will be counted, i.e. hierarchical things only count as one 
    */
  def countAll(topLevelOnly: Boolean = true)(implicit s: Session): Int = {
    if (topLevelOnly)
      Query(query.where(_.isPartOfId.isNull).length).first
    else
      Query(query.length).first
  }
    
  /** Lists all AnnotatedThings in the DB (paginated).
    *  
    * @param topLevelOnly if set to true, only top-level things will be returned
    * @param offset pagination offset
    * @param limit pagination limit (number of items to be returned)
    */
  def listAll(topLevelOnly: Boolean = true, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[AnnotatedThing] = {
    val total = countAll(topLevelOnly)
    val result = 
      if (topLevelOnly)
        query.where(_.isPartOfId.isNull).drop(offset).take(limit).list
      else
        query.drop(offset).take(limit).list
        
    Page(result, offset, limit, total)
  }
        
  /** Retrieves a single AnnotatedThing by its ID **/
  def findById(id: String)(implicit s: Session): Option[AnnotatedThing] = 
    query.where(_.id === id).firstOption
    
  /** Retrieves a single AnnotatedThing by its ID, joining with the Dataset it's contained in **/
  def findByIdWithDataset(id: String)(implicit s: Session): Option[(AnnotatedThing, Dataset)] = {
    val q = for {
      thing <- query.where(_.id === id)
      dataset <- Datasets.query if thing.datasetId === dataset.id 
    } yield (thing, dataset)
    
    q.firstOption
  }

  /** Counts the things contained in a specified dataset.
    *
    * @param datasetId the dataset ID
    * @param recursive if set to true, the count will recursively include things contained in subsets 
    * @param topLevelOnly if set to true, only top-level things will be counted, i.e. hierarchical things only count as one    
    */
  def countByDataset(datasetId: String, recursive: Boolean = true, topLevelOnly: Boolean = true)(implicit s: Session): Int = {
    val datasetIds =
      if (recursive)
        datasetId +: Datasets.walkSubsets(datasetId).map(_.id)
      else
        Seq(datasetId)

    if (topLevelOnly)
      Query(query.where(_.datasetId.inSet(datasetIds)).where(_.isPartOfId.isNull).length).first
    else
      Query(query.where(_.datasetId.inSet(datasetIds)).length).first
  }

  /** Retrieves the things contained in a specified dataset.
    *
    * @param datasetId the dataset ID
    * @param recursive if set to true, the response will recursively include things contained in subsets
    * @param topLevelOnly if set to true, only top-level things will be returned
    * @param offset pagination offset
    * @param limit pagination limit (number of items to be returned)
    */
  def findByDataset(datasetId: String, recursive: Boolean = true, topLevelOnly: Boolean = true, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[AnnotatedThing] = {
    val total = countByDataset(datasetId, recursive, topLevelOnly)
    val datasetIds = 
      if (recursive)
        datasetId +: Datasets.walkSubsets(datasetId).map(_.id)
      else
        Seq(datasetId)
        
    val result = 
      if (topLevelOnly)
        query.where(_.datasetId.inSet(datasetIds)).where(_.isPartOfId.isNull).drop(offset).take(limit).list
      else
        query.where(_.datasetId.inSet(datasetIds)).drop(offset).take(limit).list
        
    Page(result, offset, limit, total)
  }
  
  /** Returns the number of children of a specific AnnotatedThing.
    * 
    * This method only counts the direct subsets of the Dataset - it does not
    * traverse further down the hierarchy!
    */
  def countChildren(id: String)(implicit s: Session): Int =
    Query(query.where(_.isPartOfId === id).length).first

  /** Returns the children of a specific AnnotatedThing.
    * 
    * This method only returns the direct children of the AnnotatedThing - it does not
    * traverse further down in the hierarchy!
    */
  def listChildren(parentId: String, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[AnnotatedThing] = {
    val total = countChildren(parentId)
    val result = query.where(_.isPartOfId === parentId).drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }

  /** Returns all children in the hierarchy below a specific AnnotatedThing.
    * 
    * This method is similar to listChildren, but DOES traverse down the hierarchy,
    * i.e. retrieves not only the direct children, but also the childrens' children, etc. 
    */
  private[models] def walkChildren(parentId: String)(implicit s: Session): Seq[AnnotatedThing] = {
    // Note that we're making a DB request for every parent
    // TODO this could be slightly tuned by taking a list of of parentIds rather than just a single one
    
    // TODO not sure we really need to include the Datasets, possibly just the IDs are enough
    val children = query.where(_.isPartOfId === parentId).list
    if (children.isEmpty)
      children
    else
      children.flatMap(thing => thing +: walkChildren(thing.id))    
  }
  
  /** Returns the parent hierarchy of a thing, i.e. the sequence of things from this thing to the root parent **/
  def getParentHierarchy(thingId: String)(implicit s: Session): Seq[String] = {
    val parentId = query.where(_.id === thingId).where(_.isPartOfId.isNotNull).map(_.isPartOfId).firstOption
    if (parentId.isDefined) {
      parentId.get +: getParentHierarchy(parentId.get)
    } else {
      Seq.empty[String]
    }
  }
  
}
