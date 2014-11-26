package models

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.{ Tag => SlickTag }
import org.geotools.geojson.geom.GeometryJSON

case class HeatmapPoint(id: Option[Int], uri: String, lat: Double, lon: Double, weight: Int)
    
class MasterHeatmap(tag: SlickTag) extends Table[HeatmapPoint](tag, "master_heatmap") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  def uri = column[String]("uri", O.NotNull)
    
  def lat = column[Double]("lat", O.NotNull)

  def lon = column[Double]("lon", O.NotNull)
  
  def weight = column[Int]("weight", O.NotNull)
  
  def * = (id.?, uri, lat, lon, weight) <> (HeatmapPoint.tupled, HeatmapPoint.unapply)
    
}

object MasterHeatmap {
    
  private val query = TableQuery[MasterHeatmap]
  
  def create()(implicit s: Session) = {
    query.ddl.create
  }
  
  def deleteAll()(implicit s: Session) = {
    // TODO
  }
  
  def insertAll(points: Seq[HeatmapPoint])(implicit s: Session) = {
    // TODO
  }
  
  def rebuild()(implicit s: Session) = {
    val query = 
      Associations.placesToDatasets
        .where(_.location.isNotNull)
        .map(row => (row.gazetteerURI, row.location, row.count))
        .groupBy(tuple => (tuple._1, tuple._2))
        .map { case ((uri, location), group) =>
          (uri, location, group.map(_._3).sum) }
    
    val geoJson = new GeometryJSON()
    val rows = query.list.map { case (uri, geometryJson, weight) => {
      val centroid = geoJson.read(geometryJson).getCentroid.getCoordinate
      // TODO why is weight an Option[Int]?!
      HeatmapPoint(None, uri, centroid.y, centroid.x, weight.getOrElse(0))
    }}
    
    // TODO purge table
    
    // TODO insert the new heatmap
  }
    
}
