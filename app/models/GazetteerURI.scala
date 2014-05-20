package models

import play.api.db.slick.Config.driver.simple._

case class GazetteerURI(uri: String)
  
trait HasGazetteerURIColumn {
  
  implicit val statusMapper = MappedColumnType.base[GazetteerURI, String](
    { uri => uri.uri},
    { uri => GazetteerURI(uri) })
    
}
