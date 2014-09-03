package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag, Query }
import java.sql.Date
import play.api.Logger

/** Dataset model entity **/
case class Dataset(
    
  /** Internal ID in the API **/
  id: String,
    
  /** dcterms:title **/
  title: String, 
    
  /** dcterms:publisher **/
  publisher: String, 
    
  /** dcterms:license **/
  license: String, 
    
  /** time the dataset was created in the system **/
  created: Date,
    
  /** time the dataset was last modified in the system **/
  modified: Date,
    
  /** URL of the VoID file (unless imported via file upload) **/
  voidURI: Option[String], 
    
  /** dcterms:description **/
  description: Option[String],
    
  /** foaf:homepage **/
  homepage: Option[String],
  
  /** The ID of the dataset this dataset is part of (if any) **/
  isPartOf: Option[String],
    
  /** void:dataDump **/
  datadump: Option[String],
  
  /** The start of the date interval this dataset encompasses (optional) **/ 
  temporalBoundsStart: Option[Int],
  
  /** The end of the date interval this dataset encompasses (optional).
    *
    * If the dataset is dated (i.e. if it has a temporalBoundsStart value)
    * this value MUST be set. In case the thing is dated with a datestamp
    * rather than an interval, temporalBoundsEnd must be the same as
    * temporalBoundsStart
    */   
  temporalBoundsEnd: Option[Int],
  
  /** The full temporal profile and time histogram for the dataset **/
  temporalProfile: Option[String])
    
/** Dataset DB table **/
class Datasets(tag: Tag) extends Table[Dataset](tag, "datasets") {

  def id = column[String]("id", O.PrimaryKey)
  
  def title = column[String]("title", O.NotNull)
  
  def publisher = column[String]("publisher", O.NotNull)
  
  def license = column[String]("license", O.NotNull)
  
  def created = column[Date]("created", O.NotNull)
  
  def modified = column[Date]("modified", O.NotNull)
  
  def voidURI = column[String]("void_uri", O.Nullable)
  
  def description = column[String]("description", O.Nullable)
  
  def homepage = column[String]("homepage", O.Nullable)
  
  def isPartOfId = column[String]("is_part_of", O.Nullable)
  
  def datadump = column[String]("datadump", O.Nullable)
  
  def temporalBoundsStart = column[Int]("temporal_bounds_start", O.Nullable)

  def temporalBoundsEnd = column[Int]("temporal_bounds_end", O.Nullable)
  
  def temporalProfile = column[String]("temporal_profile", O.Nullable, O.DBType("text"))
  
  def * = (id, title, publisher, license, created, modified, voidURI.?, description.?, 
    homepage.?, isPartOfId.?, datadump.?, temporalBoundsStart.?, temporalBoundsEnd.?, temporalProfile.?) <> (Dataset.tupled, Dataset.unapply)
  
  /** Foreign key constraints **/
    
  def isPartOfFk = foreignKey("is_part_of_dataset_fk", isPartOfId, Datasets.query)(_.id)
  
}

/** Queries **/
object Datasets {
  
  private[models] val query = TableQuery[Datasets]
  
  /** Creates the DB table **/
  def create()(implicit s: Session) = query.ddl.create
  
  /** Inserts a single Dataset into the DB **/
  def insert(dataset: Dataset)(implicit s: Session) = query.insert(dataset)
  
  /** Inserts a list of Datasets into the DB **/
  def insertAll(dataset: Seq[Dataset])(implicit s: Session) = query.insertAll(dataset:_*)
  
  /** Updates a Dataset **/
  def update(dataset: Dataset)(implicit s: Session) = query.where(_.id === dataset.id).update(dataset)
 
  /** Deletes a Dataset **/
  def delete(id: String)(implicit s: Session) = {
	val datasetTreeIds = walkSubsets(id).map(_.id) :+ id
    query.where(_.id inSet datasetTreeIds).delete
  }
    
  /** Counts all Datasets in the DB.
    * 
    * @param topLevelOnly if set to true, only top-level datasets will be counted, i.e. hierarchical datasets only count as one 
    */
  def countAll(topLevelOnly: Boolean = true)(implicit s: Session): Int = {
    if (topLevelOnly)
      Query(query.where(_.isPartOfId.isNull).length).first
    else
      Query(query.length).first
  }

  /** Lists all Datasets in the DB (paginated).
    *  
    * @param topLevelOnly if set to true, only top-level datasets will be returned
    * @param offset pagination offset
    * @param limit pagination limit (number of items to be returned)
    */
  def listAll(topLevelOnly: Boolean = true, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[Dataset] = {
    val total = countAll(topLevelOnly)
    val result = 
      if (topLevelOnly)
        query.where(_.isPartOfId.isNull).drop(offset).take(limit).list
      else
        query.drop(offset).take(limit).list
        
    Page(result, offset, limit, total)    
  }

  /** Retrieves a single Dataset by its ID **/
  def findById(id: String)(implicit s: Session): Option[Dataset] =
    query.where(_.id === id).firstOption
    
  /** Retrieves a batch of Datasets by their ID **/
  def findAllById(ids: Seq[String])(implicit s: Session): Seq[Dataset] =
    query.where(_.id.inSet(ids)).list
    
  /** Returns the number of subsets in a specific Dataset.
    * 
    * This method only counts the direct subsets of the Dataset - it does not
    * traverse further down the hierarchy!
    */
  def countSubsets(id: String)(implicit s: Session): Int =
    Query(query.where(_.isPartOfId === id).length).first

  /** Returns the subsets of a specific Dataset.
    * 
    * This method only returns the direct subsets of the Dataset - it does not
    * traverse further down in the hierarchy!
    */
  def listSubsets(id: String)(implicit s: Session): Seq[Dataset] =
    query.where(_.isPartOfId === id).list    
 
  /** Returns all subsets in the hierarchy below a specific Dataset.
    * 
    * This method is similar to listSubsets, but it DOES traverse down the hierarchy,
    * i.e. retrieves not only the direct subsets, but also the subsets' subsets, etc. 
    */
  private[models] def walkSubsets(parentId: String)(implicit s: Session): Seq[Dataset] = {
    // Note that we're making a DB request for every parent
    // TODO this could be slightly tuned by taking a list of of parentIds rather than just a single one
    
    // TODO not sure we really need to include the Datasets, possibly just the IDs are enough
    val subsets = query.where(_.isPartOfId === parentId).list
    if (subsets.isEmpty)
      subsets
    else
      subsets.flatMap(dataset => dataset +: walkSubsets(dataset.id))    
  }

  /** Returns the parent hierarchy of a thing, i.e. the sequence of things from this thing to the root parent **/
  def getParentHierarchy(datasetId: String)(implicit s: Session): Seq[String] = {
    val parentId = query.where(_.id === datasetId).where(_.isPartOfId.isNotNull).map(_.isPartOfId).firstOption
    if (parentId.isDefined) {
      parentId.get +: getParentHierarchy(parentId.get)
    } else {
      Seq.empty[String]
    }
  }
 
}
