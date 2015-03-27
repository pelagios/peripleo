package index.objects

import com.spatial4j.core.distance.DistanceUtils
import com.spatial4j.core.shape.Rectangle
import com.spatial4j.core.shape.impl.RectangleImpl
import com.vividsolutions.jts.geom.Coordinate
import index._
import index.annotations.AnnotationReader
import models.Page
import models.core.Datasets
import models.geo.BoundingBox
import org.apache.lucene.util.Version
import org.apache.lucene.index.{ Term, MultiReader }
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader
import org.apache.lucene.search._
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.spatial.prefix.HeatmapFacetCounter
import org.apache.lucene.spatial.query.{ SpatialArgs, SpatialOperation }
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.search.spell.LuceneDictionary
import play.api.db.slick._
import org.apache.lucene.search.highlight.SimpleHTMLFormatter
import org.apache.lucene.search.highlight.Highlighter
import org.apache.lucene.search.highlight.QueryScorer
import play.api.Logger
import org.apache.lucene.search.highlight.SimpleFragmenter
import org.apache.lucene.search.highlight.SimpleSpanFragmenter
import org.apache.lucene.search.highlight.TokenSources
import org.apache.lucene.spatial.prefix.PrefixTreeFacetCounter
import org.apache.lucene.spatial.prefix.tree.NumberRangePrefixTree.UnitNRShape
import java.util.Calendar
import com.spatial4j.core.shape.Shape
import org.apache.lucene.index.IndexReaderContext
import org.apache.lucene.facet.Facets
import scala.collection.JavaConverters._
import java.util.GregorianCalendar

trait ObjectReader extends AnnotationReader {

  /** Search the index.
    *  
    * In principle, every parameter is optional. Recommended use of the constructor 
    * is via named arguments. Some combinations obviously don't make sense (e.g. a
    * bounding box restriction combined with a filter for places outside the bounding
    * box), so a minimum of common sense will be required when choosing your arguments.
    * 
    * @param query a query according to Lucene syntax
    * @param objectType restriction to specific object types (place, item, or dataset)
    * @param dataset restriction to a specific dataset
    * @param fromYear temporal restriction: start date
    * @param toYear temporal restriction: end date
    * @param places restriction to specific places (gazetteer URIs)
    * @param bbox restriction to a geographic bounding box
    * @param coord search around a specific center coordinate (requires 'radius' argument)
    * @param radius search radius around a 'coord' center
    * @param limit number of maximum hits to return
    * @param offset offset in the search result list 
    */ 
  def search(
      query:      Option[String],
      objectType: Option[IndexedObjectTypes.Value],
      dataset:    Option[String],
      gazetteer:  Option[String],
      fromYear:   Option[Int],
      toYear:     Option[Int],      
      places:     Seq[String], 
      bbox:       Option[BoundingBox],
      coord:      Option[Coordinate], 
      radius:     Option[Double],
      limit:      Int = 20, 
      offset:     Int = 0)(implicit s: Session): (Page[(IndexedObject, Option[String])], FacetTree, TimeHistogram, Heatmap) = {
     
    // The part of the query that is common for search and heatmap calculation
    val rectangle = bbox.map(b => Index.spatialCtx.makeRectangle(b.minLon, b.maxLon, b.minLat, b.maxLat))
    val baseQuery = prepareBaseQuery(objectType, dataset, gazetteer, fromYear, toYear, places, rectangle, coord, radius)
    
    // Finalize search query and build heatmap filter
    val searchQuery = {
      val search = baseQuery.clone()
      if (query.isDefined) {
        val fields = Seq(IndexFields.TITLE, IndexFields.DESCRIPTION, IndexFields.PLACE_NAME, IndexFields.ITEM_FULLTEXT).toArray       
        search.add(new MultiFieldQueryParser(fields, analyzer).parse(query.get), BooleanClause.Occur.MUST)  
      }
      search
    }
    
    val heatmapFilter = {
      if (query.isDefined) { 
        // We don't search the fulltext field for the heatmap - text-based heatmaps are handled by the annotation index
        val fields = Seq(IndexFields.TITLE, IndexFields.DESCRIPTION, IndexFields.PLACE_NAME).toArray       
        baseQuery.add(new MultiFieldQueryParser(fields, analyzer).parse(query.get), BooleanClause.Occur.MUST)  
      }
      new QueryWrapperFilter(baseQuery)
    }
    
    val placeSearcher = placeSearcherManager.acquire()
    val objectSearcher = objectSearcherManager.acquire()
    val searcher = new IndexSearcher(new MultiReader(objectSearcher.searcher.getIndexReader, placeSearcher.searcher.getIndexReader))
    
    try {   
      val (results, facets) = executeSearch(searchQuery, limit, offset, query, searcher, objectSearcher.taxonomyReader)
      
      val temporalProfile = calculateTemporalProfile(new QueryWrapperFilter(searchQuery), searcher)
      
      val heatmap = 
        calculateItemHeatmap(heatmapFilter, rectangle, searcher) // +
        // calculateAnnotationHeatmap(query, dataset, fromYear, toYear, places, rectangle, coord, radius)
        
      (results, facets, temporalProfile, heatmap)
    } finally {
      placeSearcherManager.release(placeSearcher)
      objectSearcherManager.release(objectSearcher)
    }
  }
  
  /** Constructs the query as far as it's common for search and heatmap computation **/
  private def prepareBaseQuery(
      objectType: Option[IndexedObjectTypes.Value],
      dataset:    Option[String],
      gazetteer:  Option[String],
      fromYear:   Option[Int],
      toYear:     Option[Int],      
      places:     Seq[String], 
      bbox:       Option[Rectangle],
      coord:      Option[Coordinate], 
      radius:     Option[Double])(implicit s: Session): BooleanQuery = {
    
    val q = new BooleanQuery()
      
    // Object type filter
    if (objectType.isDefined)
      q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, objectType.get.toString)), BooleanClause.Occur.MUST)
    
    // Dataset filter
    if (dataset.isDefined) {
      val datasetHierarchy = dataset.get +: Datasets.listSubsetsRecursive(dataset.get)
      if (datasetHierarchy.size == 1) {
        q.add(new TermQuery(new Term(IndexFields.ITEM_DATASET, dataset.get)), BooleanClause.Occur.MUST)        
      } else {
        val datasetQuery = new BooleanQuery()
        datasetHierarchy.foreach(id => {
          datasetQuery.add(new TermQuery(new Term(IndexFields.ITEM_DATASET, id)), BooleanClause.Occur.SHOULD)       
        })
        q.add(datasetQuery, BooleanClause.Occur.MUST)
      }
    }
    
    // Gazetteer filter
    if (gazetteer.isDefined)
      q.add(new TermQuery(new Term(IndexFields.PLACE_SOURCE_GAZETTEER, gazetteer.get.toLowerCase)), BooleanClause.Occur.MUST)
      
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
      q.add(new TermQuery(new Term(IndexFields.ITEM_PLACES, uri)), BooleanClause.Occur.MUST))
    
    // Spatial filter
    if (bbox.isDefined) {
      q.add(Index.spatialStrategy.makeQuery(new SpatialArgs(SpatialOperation.Intersects, bbox.get)), BooleanClause.Occur.MUST)
    } else if (coord.isDefined) {
      // Warning - there appears to be a bug in Lucene spatial that flips coordinates!
      val circle = Index.spatialCtx.makeCircle(coord.get.y, coord.get.x, DistanceUtils.dist2Degrees(radius.getOrElse(10), DistanceUtils.EARTH_MEAN_RADIUS_KM))
      q.add(Index.spatialStrategy.makeQuery(new SpatialArgs(SpatialOperation.IsWithin, circle)), BooleanClause.Occur.MUST)        
    }
    
    q
  }
  
  private def executeSearch(query: Query, limit: Int, offset: Int, queryString: Option[String], 
      searcher: IndexSearcher, taxonomyReader: DirectoryTaxonomyReader): (Page[(IndexedObject, Option[String])], FacetTree) = {
    
    val facetsCollector = new FacetsCollector() 
    val topDocsCollector = TopScoreDocCollector.create(offset + limit)
    searcher.search(query, MultiCollector.wrap(topDocsCollector, facetsCollector))
      
    val facetTree = new FacetTree(new FastTaxonomyFacetCounts(taxonomyReader, Index.facetsConfig, facetsCollector))      
    val total = topDocsCollector.getTotalHits
    
    val previewFormatter = new SimpleHTMLFormatter("<strong>", "</strong>")
    val scorer = new QueryScorer(query)
    val highlighter = new Highlighter(previewFormatter, scorer)
    highlighter.setTextFragmenter(new SimpleFragmenter(200))
    highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE)  
        
    val results = topDocsCollector.topDocs(offset, limit).scoreDocs.map(scoreDoc => {      
      val document = searcher.doc(scoreDoc.doc)
      
      val previewSnippet = Option(document.get(IndexFields.ITEM_FULLTEXT)).map(fulltext => {  
        val stream = TokenSources.getAnyTokenStream(searcher.getIndexReader, scoreDoc.doc, IndexFields.ITEM_FULLTEXT, analyzer)
        highlighter.getBestFragments(stream, fulltext, 4, " ... ")
      })
      
      (new IndexedObject(document), previewSnippet) 
    })
    
    (Page(results.toSeq, offset, limit, total, queryString), facetTree)
  }
  
  private def calculateTemporalProfile(filter: Filter, searcher: IndexSearcher): TimeHistogram = {
    val startCal = Index.dateRangeTree.newCal()
    startCal.set(-10000, Calendar.JANUARY, 1)
    val start = Index.dateRangeTree.toShape(startCal)
    
    val endCal = Index.dateRangeTree.newCal()
    endCal.set(3000, Calendar.DECEMBER, 31)
    val end = Index.dateRangeTree.toShape(endCal)
    
    val detailLevel = Math.max(start.getLevel(), end.getLevel()) + 1
    val facetRange = Index.dateRangeTree.toRangeShape(start, end);    
    val tempFacets = Index.temporalStrategy.calcFacets(searcher.getTopReaderContext, filter, facetRange, 4)
    
    val values = tempFacets.parents.asScala.map { case (shape, fpv) => 
      val calendar = Index.dateRangeTree.toObject(shape).asInstanceOf[Calendar]
      val year = calendar.get(Calendar.ERA) match {
        case GregorianCalendar.BC => - calendar.get(Calendar.YEAR)
        case _ => calendar.get(Calendar.YEAR)
      }
        
      val count =
        tempFacets.topLeaves +
        Option(fpv.childCounts).getOrElse(Array.empty[Int]).sum +
        Option(fpv.parentLeaves).getOrElse(0)
      (year, count)/** TODO add option to resample the histogram to a max number of buckets **/
    }
    
    TimeHistogram.create(values.toSeq, 26)
  }
  
  private def calculateItemHeatmap(filter: Filter, bbox: Option[Rectangle], searcher: IndexSearcher): Heatmap = {
    val rect = bbox.getOrElse(new RectangleImpl(-90, 90, -90, 90, null))
    
    val avgDimensionDeg = rect.getWidth + rect.getHeight
    val level = avgDimensionDeg match {
      case dim if dim < 5 => 5
      case dim if dim < 70 => 4
      case dim if dim < 470 => 3
      case _ => 2
    }

    val heatmap = HeatmapFacetCounter.calcFacets(Index.spatialStrategy, searcher.getTopReaderContext, filter, rect, level, 30000)
          
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
