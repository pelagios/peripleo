package index.annotations

import com.spatial4j.core.distance.DistanceUtils
import com.spatial4j.core.shape.impl.RectangleImpl
import com.vividsolutions.jts.geom.Coordinate
import index.{ Heatmap, Index, IndexBase, IndexFields }
import models.core.Datasets
import models.geo.BoundingBox
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.{ BooleanClause, BooleanQuery, NumericRangeQuery, Query, QueryWrapperFilter, TermQuery }
import org.apache.lucene.spatial.query.{ SpatialArgs, SpatialOperation }
import org.apache.lucene.spatial.prefix.HeatmapFacetCounter
import play.api.db.slick._
import com.spatial4j.core.shape.Rectangle
import play.api.Logger
import index.SearchParameters

trait AnnotationReader extends IndexBase {
  
  def calculateAnnotationHeatmap(
      query: Option[String] = None,
      dataset: Option[String] = None,
      fromYear: Option[Int] = None,
      toYear: Option[Int] = None,      
      places: Seq[String] = Seq.empty[String], 
      bbox: Option[Rectangle] = None,
      coord: Option[Coordinate] = None, 
      radius: Option[Double] = None, 
      level: Int
    )(implicit s: Session): Heatmap = {
    
    val q = new BooleanQuery()
    
    // Keyword query
    if (query.isDefined) {
      val fields = Seq(IndexFields.ANNOTATION_QUOTE, IndexFields.ANNOTATION_FULLTEXT_PREFIX, IndexFields.ANNOTATION_FULLTEXT_SUFFIX).toArray       
      q.add(new MultiFieldQueryParser(fields, analyzer).parse(query.get), BooleanClause.Occur.MUST)  
    }     
    
    // Dataset filter
    if (dataset.isDefined) {
      val datasetHierarchy = dataset.get +: Datasets.listSubsetsRecursive(dataset.get)
      if (datasetHierarchy.size == 1) {
        q.add(new TermQuery(new Term(IndexFields.ANNOTATION_DATASET, dataset.get)), BooleanClause.Occur.MUST)        
      } else {
        val datasetQuery = new BooleanQuery()
        datasetHierarchy.foreach(id => {
          datasetQuery.add(new TermQuery(new Term(IndexFields.ANNOTATION_DATASET, id)), BooleanClause.Occur.SHOULD)       
        })
        q.add(datasetQuery, BooleanClause.Occur.MUST)
      }
    }
    
    // Timespan filter
    if (fromYear.isDefined || toYear.isDefined) {
      val timeIntervalQuery = new BooleanQuery()
      
      if (fromYear.isDefined)
        timeIntervalQuery.add(NumericRangeQuery.newIntRange(IndexFields.DATE_TO, fromYear.get, null, true, true), BooleanClause.Occur.MUST)
        
      if (toYear.isDefined)
        timeIntervalQuery.add(NumericRangeQuery.newIntRange(IndexFields.DATE_FROM, null, toYear.get, true, true), BooleanClause.Occur.MUST)
        
      q.add(timeIntervalQuery, BooleanClause.Occur.MUST)
    }
      
    // Places filter
    places.foreach(uri =>
      q.add(new TermQuery(new Term(IndexFields.ANNOTATION_PLACE, uri)), BooleanClause.Occur.MUST))
    
    // Spatial filter
    if (bbox.isDefined) {
      q.add(Index.spatialStrategy.makeQuery(new SpatialArgs(SpatialOperation.IsWithin, bbox.get)), BooleanClause.Occur.MUST)
    } else if (coord.isDefined) {
      // Warning - there appears to be a bug in Lucene spatial that flips coordinates!
      val circle = Index.spatialCtx.makeCircle(coord.get.y, coord.get.x, DistanceUtils.dist2Degrees(radius.getOrElse(10), DistanceUtils.EARTH_MEAN_RADIUS_KM))
      q.add(Index.spatialStrategy.makeQuery(new SpatialArgs(SpatialOperation.IsWithin, circle)), BooleanClause.Occur.MUST)        
    }
      
    execute(q, bbox, level)    
  }
  
  private def execute(query: Query, bbox: Option[Rectangle], level: Int): Heatmap = {
    val searcher = annotationSearcherManager.acquire()
    val rect = bbox.getOrElse(new RectangleImpl(-90, 90, -90, 90, null))
    
    try {            
      val filter = new QueryWrapperFilter(query)
      val heatmap = HeatmapFacetCounter.calcFacets(Index.spatialStrategy, searcher.getTopReaderContext, filter, rect, level, 100000)
      
      // Heatmap grid cells with non-zero count, in the form of a tuple (x, y, count)
      val nonEmptyCells = 
        Seq.range(0, heatmap.rows).flatMap(row => {
          Seq.range(0, heatmap.columns).map(column => (column, row, heatmap.getCount(column, row)))
        }).filter(_._3 > 0)

      // Convert non-zero grid cells to map points
      val region = heatmap.region
      val (minX, minY) = (region.getMinX, region.getMinY)
      val cellWidth = region.getWidth / heatmap.columns
      val cellHeight = region.getHeight / heatmap.rows
      
      Heatmap(nonEmptyCells.map { case (x, y, count) =>
        val lon = DistanceUtils.normLonDEG(minX + x * cellWidth + cellWidth / 2)
        val lat = DistanceUtils.normLatDEG(minY + y * cellHeight + cellHeight / 2)
        (lon, lat, count)
      }, cellWidth, cellHeight)
    } finally {
      annotationSearcherManager.release(searcher)
    }     
  }
  
}
