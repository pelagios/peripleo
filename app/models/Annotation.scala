package models

import java.util.UUID
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }

/** Annotation model entity **/
case class Annotation(
    
    /** a globally unique identifier for the annotation **/ 
    uuid: UUID, 
    
    /** the ID of the dataset the annotation is in **/
    dataset: String, 
    
    /** the ID of the annotated thing the annotation is attached to **/
    annotatedThing: String,
    
    /** the gazetteer URI in the annotation 'body' **/
    gazetteerURI: String,
    
    /** any additional text content in the annotation 'body' (such as a transcription) **/
    quote: Option[String],
    
    /** the offset in the annotated thing - really only a measure to compute distance between annotations! **/ 
    offset: Option[Int])

/** Annotation DB table **/
class Annotations(tag: SlickTag) extends Table[Annotation](tag, "annotations") {

  def uuid = column[UUID]("uuid", O.PrimaryKey)
  
  def datasetId = column[String]("dataset", O.NotNull)
  
  def annotatedThingId = column[String]("annotated_thing", O.NotNull)
  
  def gazetteerURI = column[String]("gazetteer_uri", O.NotNull)
  
  def quote = column[String]("quote", O.Nullable)
  
  def offset = column[Int]("offset", O.Nullable)
  
  def * = (uuid, datasetId, annotatedThingId, gazetteerURI, quote.?, offset.?) <> (Annotation.tupled, Annotation.unapply)
  
  /** Foreign key constraints **/
  
  def datasetFk = foreignKey("dataset_fk", datasetId, Datasets.query)(_.id)
  
  def annotatedThingFk = foreignKey("annotated_thing_fk", annotatedThingId, AnnotatedThings.query)(_.id)
  
  /** Indices **/
  
  def datasetIdx = index("idx_annotations_by_dataset", datasetId, unique = false)
  
  def annotatedThingIdx = index("idx_annotations_by_thing", annotatedThingId, unique = false)
  
}

/** Queries **/
object Annotations {
  
  private[models] val query = TableQuery[Annotations]
  
  /** Creates the DB table **/
  def create()(implicit s: Session) = query.ddl.create
  
  /** Inserts a list of Annotations into the DB **/
  def insertAll(annotations: Seq[Annotation])(implicit s: Session) = {
    query.insertAll(annotations:_*)
    
    // Update aggregation table stats
    AggregatedView.recompute(annotations)
  }
    
  /** Deletes annotations from the specified datasets **/ 
  def deleteForDatasets(ids: Seq[String])(implicit s: Session) =
    query.where(_.datasetId inSet ids).delete
  
  /** Updates an Annotation **/
  def update(annotation: Annotation)(implicit s: Session) = 
    query.where(_.uuid === annotation.uuid).update(annotation)
    
  /** Counts all Annotations in the DB **/
  def countAll()(implicit s: Session): Int = 
    Query(query.length).first

  /** Lists all Annotations in the DB (paginated) **/
  def listAll(offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[Annotation] = {
    val total = countAll()
    val result = query.drop(offset).take(limit).list
    Page(result, offset, limit, total)
  }

  /** Retrieves a single Annotation by its UUID **/
  def findByUUID(uuid: UUID)(implicit s: Session): Option[Annotation] = 
    query.where(_.uuid === uuid).firstOption
   
  /** Counts the Annotations contained in a specified dataset.
    *
    * @param datasetId the dataset ID
    * @param recursive if set to true, the count will recursively include Annotations contained in subsets 
    */
  def countByDataset(datasetId: String, recursive: Boolean = true)(implicit s: Session): Int = {
    if (recursive) {
      val allDatasetIds = datasetId +: Datasets.listSubsetsRecursive(datasetId)
      Query(query.where(_.datasetId.inSet(allDatasetIds)).length).first
    } else {
      Query(query.where(_.datasetId === datasetId).length).first      
    }
  }
    
  /** Retrieves the Annotations contained in a specified dataset.
    *
    * @param datasetId the dataset ID
    * @param recursive if set to true, the response will recursively include Annotations contained in subsets
    * @param offset pagination offset
    * @param limit pagination limit (number of items to be returned)
    */
  def findByDataset(datasetId: String, recursive: Boolean = true, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[Annotation] = {
    val total = countByDataset(datasetId, recursive)
    val result = 
      if (recursive) {
        val allDatasetIds = datasetId +: Datasets.listSubsetsRecursive(datasetId)
        query.where(_.datasetId.inSet(allDatasetIds)).drop(offset).take(limit).list
      } else {
        query.where(_.datasetId === datasetId).drop(offset).take(limit).list  
      }
    
    Page(result, offset, limit, total)
  }

  /** Counts the Annotations attached to a specified thing.
    *
    * @param thingId the thing ID
    * @param recursive if set to true, the count will recursively include Annotations contained in sub-things
    */
  def countByAnnotatedThing(thingId: String, recursive: Boolean = true)(implicit s: Session): Int = {
    if (recursive) {
      val allThingIds = thingId +: AnnotatedThings.listChildrenRecursive(thingId)
      Query(query.where(_.annotatedThingId.inSet(allThingIds)).length).first
    } else {
      Query(query.where(_.annotatedThingId === thingId).length).first
    }
  }

  /** Retrieves the Annotations attached to a specified thing.
    *
    * @param thingId the thingID
    * @param recursive if set to true, the response will recursively include Annotations contained in sub-things
    * @param offset pagination offset
    * @param limit pagination limit (number of items to be returned)
    */
  def findByAnnotatedThing(thingId: String, recursive: Boolean = true, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[Annotation] = {
    val total = countByAnnotatedThing(thingId, recursive)
    val result = if (recursive) {
      val allThingIds = thingId +: AnnotatedThings.listChildrenRecursive(thingId)
      query.where(_.annotatedThingId.inSet(allThingIds)).drop(offset).take(limit).list
    } else {
      query.where(_.annotatedThingId === thingId).drop(offset).take(limit).list      
    }
    
    Page(result, offset, limit, total)
  }
  
}
