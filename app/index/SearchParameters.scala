package index

import index.objects.IndexedObjectTypes
import models.geo.BoundingBox
import com.vividsolutions.jts.geom.Coordinate
  
object DateFilterMode extends Enumeration {
      
  val INTERSECTS, CONTAINS = Value
  
}

/** A wrapper around a full complement of search arguments **/
case class SearchParameters(
  /** Keyword/ phrase query **/
  query:             Option[String],
  
  /** Object type filter **/
  objectTypes:        Seq[IndexedObjectTypes.Value],
  
  /** Inverse object type that excludes specific types **/
  excludeObjectTypes:        Seq[IndexedObjectTypes.Value],  
  
  /** Dataset filter **/
  datasets:          Seq[String],
  
  /** Inverse dataset filter that excludes specific sets **/
  excludeDatasets:   Seq[String],
  
  /** Gazetteer filter **/
  gazetteers:        Seq[String],
  
  /** Inverse gazetteer filter that excludes specific gazetteers **/
  excludeGazetteers: Seq[String],
  
  /** Language filter **/
  languages: Seq[String], 
  
  /** Inverse language filter **/
  excludeLanguages: Seq[String],
  
  /** Date filter (start year) **/
  from:              Option[Int],
  
  /** Date filter (end year) **/
  to:                Option[Int],
  
  /** Date range filtering mode - match intersecting vs. contained ranges **/
  dateFilterMode:     DateFilterMode.Value,
  
  /** Restriction to specific place **/  
  places:            Seq[String],
  
  /** Geo search filter: objects overlapping bounding box **/
  bbox:              Option[BoundingBox],
  
  /** Geo search filter: objects around a coordinate **/
  coord:             Option[Coordinate],
  
  /** Geo search filter: radius around coordinate **/
  radius:            Option[Double],
  
  /** Pagination limit (i.e. max. number of items returned **/
  limit:             Int,
  
  /** Pagination offset (i.e. number of items discarded **/
  offset:            Int) {
  
  private var _error: Option[String] = None 
  
  lazy val error = { 
    if (_error.isEmpty)
      isValid
      
    _error
  }
    
  /** Query is valid if at least one param is set **/
  def isValid: Boolean = {
    val requiresOneOf = 
      Seq(query, from, to, bbox, coord).map(_.isDefined) ++
      Seq(places, objectTypes, excludeObjectTypes, datasets, excludeDatasets, gazetteers, excludeGazetteers).map(_.size > 0)
      
    if (requiresOneOf.forall(isTrue => isTrue)) {
      _error = Some("at least one of the following parameters is required: query, from, to, bbox, coord, places, objectTypes, datasets, gazetteers")
      false
    } else if (from.isDefined && to.isDefined && from.get > to.get) {
      _error = Some("from parameter must be less than to parameter") 
      false
    } else {
      true
    }
  }

}

object SearchParameters {
  
  /** Helper: parameters for fetching all data for a specific place **/
  def forPlace(uri: String, limit: Int, offset: Int) =
    SearchParameters(
      None,
      Seq.empty[IndexedObjectTypes.Value],
      Seq.empty[IndexedObjectTypes.Value],
      Seq.empty[String],
      Seq.empty[String],
      Seq.empty[String],
      Seq.empty[String],
      Seq.empty[String],
      Seq.empty[String],
      None,
      None,
      DateFilterMode.INTERSECTS,
      Seq(uri),
      None,
      None,
      None,
      limit,
      offset)
  
}