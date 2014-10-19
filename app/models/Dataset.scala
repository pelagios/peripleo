package models

import java.sql.Date
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag, Query }
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
class Datasets(tag: SlickTag) extends Table[Dataset](tag, "datasets") {

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
    
  def temporalBoundsStart = column[Int]("temporal_bounds_start", O.Nullable)

  def temporalBoundsEnd = column[Int]("temporal_bounds_end", O.Nullable)
  
  def temporalProfile = column[String]("temporal_profile", O.Nullable, O.DBType("text"))
  
  def * = (id, title, publisher, license, created, modified, voidURI.?, description.?, 
    homepage.?, isPartOfId.?, temporalBoundsStart.?, temporalBoundsEnd.?, temporalProfile.?) <> (Dataset.tupled, Dataset.unapply)
  
  /** Foreign key constraints **/
    
  def isPartOfFk = foreignKey("is_part_of_dataset_fk", isPartOfId, Datasets.query)(_.id)
  
}

/** Queries **/
object Datasets {
  
  private[models] val query = TableQuery[Datasets]
  
  /** Creates the DB table **/
  def create()(implicit s: Session) = query.ddl.create
  
  /** Recomputes the temporal profile for this dataset and all datasets above it in the hierarchy **/
  def recomputeTemporalProfileRecursive(dataset: Dataset)(implicit s: Session): Unit = {
    val datasetsAbove = getParentHierarchy(dataset.id) 
    val allDatasets = dataset +: findByIds(datasetsAbove)
    
    // Note: this will fetch subsets and things for each dataset from the DB in every loop, so could be optimized
    // but usually we won't have huge numbers of datasets, so optimization wouldn't have much impact
    allDatasets.foreach(recomputeTemporalProfile(_))
  }

  private def recomputeTemporalProfile(dataset: Dataset)(implicit s: Session) = {
    Logger.info("Recomputing temporal profile for dataset " + dataset.title)
    
    // Grab all toplevel things in this dataset and its subsets
    val datedThings = AnnotatedThings.findByDataset(dataset.id, true, true).items.filter(_.temporalBoundsStart.isDefined)
    
    // Compute the temporal profile from the things
    val (tempBoundsStart, tempBoundsEnd, tempProfile) = 
      if (datedThings.isEmpty) {
        (None, None, None)
      } else {
        val boundsStart = datedThings.map(_.temporalBoundsStart.get).min
        val boundsEnd = datedThings.map(_.temporalBoundsEnd.get).max
        val profile = new TemporalProfile(datedThings.map(thing => (thing.temporalBoundsStart.get, thing.temporalBoundsEnd.get)))
        (Some(boundsStart), Some(boundsEnd), Some(profile.toString))
      }
    
    // Update the DB record
    val updatedDataset = Dataset(dataset.id, dataset.title, dataset.publisher, dataset.license,
      dataset.created, new Date(System.currentTimeMillis), dataset.voidURI, dataset.description, 
      dataset.homepage, dataset.isPartOf, tempBoundsStart, tempBoundsEnd, tempProfile)
    
    update(updatedDataset) 
  }
  
  /** Inserts a single Dataset into the DB **/
  def insert(dataset: Dataset)(implicit s: Session) = query.insert(dataset)
  
  /** Inserts a list of Datasets into the DB **/
  def insertAll(dataset: Seq[Dataset])(implicit s: Session) = query.insertAll(dataset:_*)
  
  /** Updates a Dataset **/
  def update(dataset: Dataset)(implicit s: Session) = query.where(_.id === dataset.id).update(dataset)
    
  /** Deletes a list of Datasets **/
  def delete(ids: Seq[String])(implicit s: Session) =
    query.where(_.id inSet ids).delete
    
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
  def listAll(topLevelOnly: Boolean = true, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[(Dataset, Seq[DatasetDumpfile])] = {
    val total = countAll(topLevelOnly)
    val datasets =
      if (topLevelOnly)
        query.where(_.isPartOfId.isNull).drop(offset).take(limit).list
      else 
        query.drop(offset).take(limit).list
     
    val dumpfiles = 
      DatasetDumpfiles.query.where(_.datasetId inSet datasets.map(_.id)).list.groupBy(_.datasetId)
          
    val result = datasets.map(dataset =>
      (dataset, dumpfiles.get(dataset.id).getOrElse(Seq.empty[DatasetDumpfile])))
             
    Page(result, offset, limit, total)    
  }

  /** Retrieves a Datasets by ID **/
  def findById(id: String)(implicit s: Session): Option[Dataset] =
    query.where(_.id === id).firstOption
  
  /** Retrieves a batch of Datasets by their ID **/
  def findByIds(ids: Seq[String])(implicit s: Session): Seq[Dataset] =
    query.where(_.id.inSet(ids)).list
  
  /** Retrieves a single Dataset by its ID **/
  def findByIdWithDumpfiles(id: String)(implicit s: Session): Option[(Dataset, Seq[DatasetDumpfile])] = {
    val dataset = query.where(_.id === id).firstOption
    dataset.map(d =>
      (d, DatasetDumpfiles.query.where(_.datasetId === d.id).list))
  }
    
  /** Returns the number of direct subsets in a specific Dataset.
    * 
    * This method only counts the direct subsets - it does not traverse further down the hierarchy!
    */
  def countSubsets(id: String)(implicit s: Session): Int =
    Query(query.where(_.isPartOfId === id).length).first

  /** Returns the direct subsets of a specific Dataset.
    * 
    * This method only returns the direct subsets - it does not traverse further down in the hierarchy!
    */
  def listSubsets(id: String)(implicit s: Session): Seq[Dataset] =
    query.where(_.isPartOfId === id).list    
    
  /** Returns all subsets in the hierarchy below a specific Dataset.
    * 
    * This method is similar to listSubsets, but it DOES traverse down the hierarchy,
    * i.e. retrieves not only the direct subsets, but also the subsets' subsets, etc. 
    */
  def listSubsetsRecursive(parentId: String)(implicit s: Session): Seq[String] =
    listSubsetsRecursive(Seq(parentId))
    
  private def listSubsetsRecursive(parentIds: Seq[String])(implicit s: Session): Seq[String] = {
    val subsets = query.where(_.isPartOfId inSet parentIds).map(_.id).list
    if (subsets.isEmpty)
      subsets
    else
      subsets ++ listSubsetsRecursive(subsets)
  }

  /** Returns the parent hierarchy of a thing, i.e. the sequence of things from this thing to the root parent. 
    *
    * Note: first element in the list is the direct parent, last is the top-most root.
    */
  def getParentHierarchy(datasetId: String)(implicit s: Session): Seq[String] = {
    val parentId = query.where(_.id === datasetId).where(_.isPartOfId.isNotNull).map(_.isPartOfId).firstOption
    if (parentId.isDefined) {
      parentId.get +: getParentHierarchy(parentId.get)
    } else {
      Seq.empty[String]
    }
  }
  
  def getParentHierarchyWithDatasets(dataset: Dataset)(implicit s: Session): Seq[Dataset] = {
    if (dataset.isPartOf.isEmpty) {
      Seq.empty[Dataset]
    } else {
      val parent = query.where(_.id === dataset.isPartOf.get).firstOption
      if (parent.isDefined)
        parent.get +: getParentHierarchyWithDatasets(parent.get)
      else
        Seq.empty[Dataset]
    }
  }
 
}
