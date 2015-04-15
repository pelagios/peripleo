package index

import org.apache.lucene.document.{ FieldType, TextField }

object IndexFields {
  
  /** General fields **/

  val ID = "id"
        
  val TITLE = "title"
    
  val DESCRIPTION = "description"
  
  val OBJECT_TYPE = "type"
  
  val SOURCE_DATASET = "source_dataset"    

  val HOMEPAGE = "homepage"
    
  val IS_PART_OF = "is_part_of"

  val DATE_FROM = "date_from"
    
  val DATE_TO = "date_to"
  
  val DATE_POINT = "date_xy"
    
  val GEOMETRY = "geometry"
  
  // val CONVEX_HULL = "convex_hull"
  
  val BOUNDING_BOX = "bbox"
  
  val PLACE_URI = "place_uri" 
    
    
  /** Item-specific fields **/
    

    
  // val ITEM_PLACES = "place_uri"
  
  val ITEM_FULLTEXT = "fulltext"
  
  // val ITEM_FULLTEXT_OFFSETS = "fulltext_offsets"
  
    
  /** Place-specific fields **/
    
  // val PLACE_URI = "uri"
    
  // val PLACE_SOURCE_GAZETTEER = "source_gazetteer"

  val PLACE_NAME = "name"
    
  val PLACE_MATCH = "match"
    
  val PLACE_AS_JSON = "place"
  
  
  /** Annotation-specific fields **/
    
  // val ANNOTATION_DATASET = "dataset"
  
  val ANNOTATION_THING = "annotated_thing"
  
  // val ANNOTATION_PLACE = "place_uri"
  
  val ANNOTATION_QUOTE = "quote"
  
  val ANNOTATION_FULLTEXT_PREFIX = "fulltext_prefix"
  
  val ANNOTATION_FULLTEXT_SUFFIX = "fulltext_suffix"
    
}
