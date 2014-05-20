package models

import play.api.db.slick.Config.driver.simple._

/** GazetteerURI model class.
  * 
  * Note: a gazetteer URI is really just a string, but we
  * wrap it into a class, so we can later identify it properly (and in
  * a type-safe manner), primarily for the purposes of JSON serialization. 
  */
case class GazetteerURI(uri: String)
  
/** DB type mapper **/
trait HasGazetteerURIColumn {
  
  implicit val statusMapper = MappedColumnType.base[GazetteerURI, String](
    { uri => uri.uri},
    { uri => GazetteerURI(uri) })
    
}
