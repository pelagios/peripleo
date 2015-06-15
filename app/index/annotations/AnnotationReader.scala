package index.annotations

import com.spatial4j.core.distance.DistanceUtils
import com.spatial4j.core.shape.Rectangle
import com.spatial4j.core.shape.impl.RectangleImpl
import com.vividsolutions.jts.geom.Coordinate
import global.Global
import index.{ Heatmap, Index, IndexBase, IndexFields, SearchParameters }
import index.places.IndexedPlaceNetwork
import models.core.{ AnnotatedThings, Datasets }
import models.geo.BoundingBox
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.taxonomy.{ FastTaxonomyFacetCounts, TaxonomyReader }
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager.SearcherAndTaxonomy
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.{ BooleanClause, BooleanQuery, IndexSearcher, NumericRangeQuery, Query, QueryWrapperFilter, TermQuery }
import org.apache.lucene.spatial.query.{ SpatialArgs, SpatialOperation }
import org.apache.lucene.spatial.prefix.HeatmapFacetCounter
import play.api.Play
import play.api.Play.current
import play.api.db.slick._

trait AnnotationReader extends IndexBase {
  
  def calculateTopPlaces(query: Query, limit: Int, searcher: IndexSearcher, taxonomyReader: TaxonomyReader): Seq[(IndexedPlaceNetwork, Int)] = {
    val fc = new FacetsCollector()
    searcher.search(query, fc)
    
    val facets = new FastTaxonomyFacetCounts(taxonomyReader, Index.facetsConfig, fc)
    
    val topURIs = 
      Option(facets.getTopChildren(limit, IndexFields.PLACE_URI)).map(result => {
        result.labelValues.toSeq.map(lv => (lv.label, lv.value.intValue))
      }).getOrElse(Seq.empty[(String, Int)])
      
    topURIs.map { case (uri, count) =>
      Global.index.findNetworkByPlaceURI(uri).map((_, count)) }.flatten
  }
  
  def getSnippets(thingId: String, phrase: String, place: Option[String], limit: Int, searcher: IndexSearcher): Seq[String] = {
    DB.withSession { implicit session: Session =>
      val rootId = AnnotatedThings.getParentHierarchy(thingId).lastOption.getOrElse(thingId)
      val allIds = rootId +: AnnotatedThings.listChildrenRecursive(rootId)
      
      val idQuery = 
        if (allIds.size > 1) {
          val q = new BooleanQuery()
          allIds.foreach(id => q.add(new TermQuery(new Term(IndexFields.ANNOTATION_THING, id)), BooleanClause.Occur.SHOULD))
          q
        } else {
          new TermQuery(new Term(IndexFields.ANNOTATION_THING, allIds.head))
        }
     
      val query = new BooleanQuery()
      query.add(idQuery, BooleanClause.Occur.MUST)
      
      val fields = Seq(
        IndexFields.ANNOTATION_QUOTE,
        IndexFields.ANNOTATION_FULLTEXT_PREFIX,
        IndexFields.ANNOTATION_FULLTEXT_SUFFIX).toArray
            
      query.add(new MultiFieldQueryParser(fields, analyzer).parse(phrase), BooleanClause.Occur.MUST)  
      
      if (place.isDefined)
        query.add(new TermQuery(new Term(IndexFields.PLACE_URI, place.get)), BooleanClause.Occur.MUST)
      
      val topDocs = searcher.search(query, 3)
    
      topDocs.scoreDocs.map(scoreDoc => { 
        val annotation = new IndexedAnnotation(searcher.doc(scoreDoc.doc))
        val text = annotation.text 
        
        // Trim around the search term
        val idx = text.indexOf(phrase)
        val start = Math.max(0, idx - 200)
        val end = Math.min(text.size, idx + 200)
        
        text.substring(start, end).replace(phrase, "<strong>" + phrase + "</strong>")
      })
      
    }
  }
  
  def calculateAnnotationHeatmap(
      query: Option[String] = None,
      datasets: Seq[String] = Seq.empty[String],
      excludeDatasets: Seq[String] = Seq.empty[String],
      fromYear: Option[Int] = None,
      toYear: Option[Int] = None,      
      places: Seq[String] = Seq.empty[String], 
      bbox: Option[Rectangle] = None,
      coord: Option[Coordinate] = None, 
      radius: Option[Double] = None, 
      level: Int,
      searcher: SearcherAndTaxonomy
    )(implicit s: Session): Heatmap = {
    
    val q = new BooleanQuery()
    
    // Keyword query
    if (query.isDefined) {
      val fields = Seq(IndexFields.ANNOTATION_QUOTE, IndexFields.ANNOTATION_FULLTEXT_PREFIX, IndexFields.ANNOTATION_FULLTEXT_SUFFIX).toArray       
      q.add(new MultiFieldQueryParser(fields, analyzer).parse(query.get), BooleanClause.Occur.MUST)  
    }     
    
    /* Dataset filter
    if (dataset.isDefined) {
      val datasetHierarchy = dataset.get +: Datasets.listSubsetsRecursive(dataset.get)
      if (datasetHierarchy.size == 1) {
        q.add(new TermQuery(new Term(IndexFields.SOURCE_DATASET, dataset.get)), BooleanClause.Occur.MUST)        
      } else {
        val datasetQuery = new BooleanQuery()
        datasetHierarchy.foreach(id => {
          datasetQuery.add(new TermQuery(new Term(IndexFields.SOURCE_DATASET, id)), BooleanClause.Occur.SHOULD)       
        })
        q.add(datasetQuery, BooleanClause.Occur.MUST)
      }
    }
    */
    
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
      q.add(new TermQuery(new Term(IndexFields.PLACE_URI, uri)), BooleanClause.Occur.MUST))
    
    // Spatial filter
    if (bbox.isDefined) {
      q.add(Index.rptStrategy.makeQuery(new SpatialArgs(SpatialOperation.IsWithin, bbox.get)), BooleanClause.Occur.MUST)
    } else if (coord.isDefined) {
      // Warning - there appears to be a bug in Lucene spatial that flips coordinates!
      val circle = Index.spatialCtx.makeCircle(coord.get.y, coord.get.x, DistanceUtils.dist2Degrees(radius.getOrElse(10), DistanceUtils.EARTH_MEAN_RADIUS_KM))
      q.add(Index.rptStrategy.makeQuery(new SpatialArgs(SpatialOperation.IsWithin, circle)), BooleanClause.Occur.MUST)        
    }
      
    execute(q, bbox, level, searcher)    
  }
  
  private def execute(query: Query, bbox: Option[Rectangle], level: Int, searcher: SearcherAndTaxonomy): Heatmap = {
    val rect = bbox.getOrElse(new RectangleImpl(-90, 90, -90, 90, null))
    
    val filter = new QueryWrapperFilter(query)
    val heatmap = HeatmapFacetCounter.calcFacets(Index.rptStrategy, searcher.searcher.getTopReaderContext, filter, rect, level, 100000)
      
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
  }
  
}
