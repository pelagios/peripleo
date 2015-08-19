package index

import org.apache.lucene.document.{ FieldType, TextField }

object IndexFields {
  
  /** Fields for internal use **/

  val BOOST = "boost"
  
  
  /** General fields **/

  val ID = "id"
        
  val TITLE = "title"
    
  val DESCRIPTION = "description"
  
  val OBJECT_TYPE = "type"
  
  val SOURCE_DATASET = "source_dataset"
  
  val DATASET_HIERARCHY = "dataset_hierarchy"

  val HOMEPAGE = "homepage"
  
  val DEPICTION = "depiction" 
  
  val LANGUAGE = "lang"
    
  val IS_PART_OF = "is_part_of"

  val DATE_FROM = "date_from"
    
  val DATE_TO = "date_to"
  
  val DATE_POINT = "date_xy"
    
  val GEOMETRY = "geometry"
  
  val BOUNDING_BOX = "bbox"
  
  val PLACE_URI = "place_uri" 
    
    
  /** Item-specific fields **/
  
  val ITEM_FULLTEXT = "fulltext"
 
  
    
  /** Place-specific fields **/
  
  val SEED_URI = "seed_uri"
  
  val PLACE_NAME = "name"
    
  val PLACE_MATCH = "match"
    
  val PLACE_AS_JSON = "place"
  
  
  /** Annotation-specific fields **/
  
  val ANNOTATION_THING = "annotated_thing"
  
  val ANNOTATION_QUOTE = "quote"
  
  val ANNOTATION_FULLTEXT_PREFIX = "fulltext_prefix"
  
  val ANNOTATION_FULLTEXT_SUFFIX = "fulltext_suffix"
    
}
