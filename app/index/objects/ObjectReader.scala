package index.objects

import com.spatial4j.core.shape.Rectangle
import com.spatial4j.core.distance.DistanceUtils
import com.vividsolutions.jts.geom.Coordinate
import index._
import index.annotations.AnnotationReader
import java.util.{ Calendar, GregorianCalendar }
import models.Page
import models.core.Datasets
import models.geo.BoundingBox
import org.apache.lucene.util.Version
import org.apache.lucene.index.{ Term, MultiReader }
import org.apache.lucene.facet.{ DrillDownQuery, DrillSideways, Facets, FacetsCollector }
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader
import org.apache.lucene.search._
import org.apache.lucene.search.highlight.{ Highlighter, SimpleHTMLFormatter, SimpleFragmenter, TokenSources, QueryScorer }
import org.apache.lucene.queries.function.ValueSource
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.spatial.prefix.HeatmapFacetCounter
import org.apache.lucene.spatial.prefix.tree.NumberRangePrefixTree.UnitNRShape
import org.apache.lucene.spatial.query.{ SpatialArgs, SpatialOperation }
import play.api.db.slick._
import scala.collection.JavaConverters._
import play.api.Logger

trait ObjectReader extends AnnotationReader {
  
  private val PREVIEW_SNIPPET_SIZE = 200
  
  private val PREVIEW_MAX_NUM_SNIPPETS = 4
  
  private val PREVIEW_SNIPPET_SEPARATOR = " ... "
  
  /** Helper function that adds time query condition if defined **/
  private def addTimeFilter(query: BooleanQuery, from: Option[Int], to: Option[Int]) = {
    if (from.isDefined || to.isDefined) {
      val timeIntervalQuery = new BooleanQuery()
      
      if (from.isDefined)
        timeIntervalQuery.add(NumericRangeQuery.newIntRange(IndexFields.DATE_TO, from.get, null, true, true), BooleanClause.Occur.MUST)
        
      if (to.isDefined)
        timeIntervalQuery.add(NumericRangeQuery.newIntRange(IndexFields.DATE_FROM, null, to.get, true, true), BooleanClause.Occur.MUST)
       
      query.add(timeIntervalQuery, BooleanClause.Occur.MUST)
    }    
  }
  
  /** TODO make this more sophisticated **/
  private def getHeatmapLevelForRect(rect: Rectangle): Int = {
    Math.min(rect.getWidth, rect.getHeight) match {
      case dim if dim < 5 => 5
      case dim if dim < 30 => 4
      case dim if dim < 300 => 3
      case _ => 2
    } 
  }

  /** Execute a search across places and annotated things. 
    * 
    * @param params the search and filter parameters
    * @param includeFacets set to true to include facet counts in the results
    * @param includeSnippets set to true to include fulltext preview snippets in the results
    * @param includeTimeHistogram set to true to include the time histogram (temporal facets) in the results
    * @param includeHeatmap set to true to include result heatmap (2d spatial facets) in the results
    */
  def search(params: SearchParameters, includeFacets: Boolean, includeSnippets: Boolean,
      includeTimeHistogram: Boolean, includeHeatmap: Boolean)(implicit s: Session): 
      (Page[(IndexedObject, Option[String])], Option[FacetTree], Option[TimeHistogram], Option[Heatmap]) = {
     
    val rectangle = params.bbox.map(b => Index.spatialCtx.makeRectangle(b.minLon, b.maxLon, b.minLat, b.maxLat))
    
    // The base query is the part of the query that is the same for search, time histogram and heatmap calculation
    val (baseQuery, valueSource) = prepareBaseQuery(params.objectType, params.dataset, 
      params.gazetteer, params.places, rectangle, params.coord, params.radius)
      
    // Finalize search query and time histogram filter
    val (searchQuery, timeHistogramFilter) = {
      
      // In both cases, we want to include fulltext search...
      val baseSearchQuery = baseQuery.clone()
      if (params.query.isDefined) {
        val fields = Seq(IndexFields.TITLE, IndexFields.DESCRIPTION, IndexFields.PLACE_NAME, IndexFields.ITEM_FULLTEXT).toArray       
        baseSearchQuery.add(new MultiFieldQueryParser(fields, analyzer).parse(params.query.get), BooleanClause.Occur.MUST)  
      }
      
      // ...but we only want to restrict the SEARCH by time interval - the histogram should count all the facets
      val timeHistogramFilter = 
        if (includeTimeHistogram)
          Some(new QueryWrapperFilter(baseSearchQuery.clone()))
        else
          None
          
      addTimeFilter(baseSearchQuery, params.from, params.to)

      (baseSearchQuery, timeHistogramFilter)
    }
    
    // Finalize the heatmap filter (we don't search item fulltext, but want the time filter)
    val heatmapFilter = {
      if (includeHeatmap) {
        val h = baseQuery.clone()
      
        addTimeFilter(h, params.from, params.to)
      
        if (params.query.isDefined) { 
          val fields = Seq(IndexFields.TITLE, IndexFields.DESCRIPTION, IndexFields.PLACE_NAME).toArray       
          h.add(new MultiFieldQueryParser(fields, analyzer).parse(params.query.get), BooleanClause.Occur.MUST)  
        }
      
        Some(new QueryWrapperFilter(h))
      } else {
        None
      }
    }

    
    val placeSearcher = placeSearcherManager.acquire()
    val objectSearcher = objectSearcherManager.acquire()
    val searcher = params.objectType match { // Just a bit of optimization
      case Some(typ) if typ == IndexedObjectTypes.PLACE => // Search place index only
        placeSearcher.searcher
        
      case Some(typ) => // Items or Datasets - search object index only
        objectSearcher.searcher
        
      case None => // Search both indices  
        new IndexSearcher(new MultiReader(objectSearcher.searcher.getIndexReader, placeSearcher.searcher.getIndexReader))
    } 
    
    try {   
      // Search & facet counts
      val (results, facets) = 
        executeSearch(searchQuery, params.places, params.limit, params.offset, searcher, objectSearcher.taxonomyReader,
          valueSource, includeFacets, includeSnippets)
      
      // Time histogram computation
      val temporalProfile = timeHistogramFilter.map(filter => calculateTemporalProfile(filter, searcher))
      
      // Heatmap computation
      val heatmap = heatmapFilter.map(filter => {
        val rect = rectangle.getOrElse(Index.spatialCtx.makeRectangle(-90, 90, -90, 90))
        val level = getHeatmapLevelForRect(rect)
        
        if (params.query.isDefined) {
          // If there is a query phrase, we include the annotation heatmap 
          calculateItemHeatmap(filter, rect, level, searcher) +
          calculateAnnotationHeatmap(params.query, params.dataset, params.from, params.to, params.places, rectangle,
            params.coord, params.radius, level)
        } else {
          // Otherwise, we only need the item-based heatmap
          calculateItemHeatmap(filter, rect, level, searcher)
        }
      })
      
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
      places:     Seq[String], 
      bbox:       Option[Rectangle],
      coord:      Option[Coordinate], 
      radius:     Option[Double])(implicit s: Session): (BooleanQuery, Option[ValueSource]) = {
    
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
    
    // Places filter
    if (places.size  > 0) {
      // For places we always want to use drill sideways 
    }
      
    // Spatial filter
    val valuesource = {
      if (bbox.isDefined) {
        q.add(Index.bboxStrategy.makeQuery(new SpatialArgs(SpatialOperation.BBoxIntersects, bbox.get)), BooleanClause.Occur.MUST)
        Some(Index.bboxStrategy.makeOverlapRatioValueSource(bbox.get, 0.5))
      } else if (coord.isDefined) {
        val circle = Index.spatialCtx.makeCircle(coord.get.x, coord.get.y, DistanceUtils.dist2Degrees(radius.getOrElse(10), DistanceUtils.EARTH_MEAN_RADIUS_KM))
        q.add(Index.rptStrategy.makeQuery(new SpatialArgs(SpatialOperation.IsWithin, circle)), BooleanClause.Occur.MUST)
        
        // TODO create & return distance value source
        None
      } else {
        None       
      }
    } 

    (q, valuesource)
  }
  
  private def executeDrillSideways(query: BooleanQuery, places: Seq[String], limit: Int, offset: Int, searcher: IndexSearcher,
    taxonomyReader: DirectoryTaxonomyReader): (TopDocs, Option[Facets]) = {
    
    val drilldownQuery = new DrillDownQuery(Index.facetsConfig, query)
    places.foreach(p => drilldownQuery.add(IndexFields.ITEM_PLACES, p))
          
    val ds = new DrillSideways(searcher, Index.facetsConfig, taxonomyReader)
    val dsResult = ds.search(drilldownQuery, offset + limit)
      
    (dsResult.hits, Some(dsResult.facets))
  }
  
  private def executeStandardQuery(query: BooleanQuery, places: Seq[String], limit: Int, offset: Int, searcher: IndexSearcher, 
      taxonomyReader: DirectoryTaxonomyReader, valueSource: Option[ValueSource], includeFacets: Boolean): (TopDocs, Option[Facets]) = {
    
    // Places filter
    places.foreach(uri =>
      query.add(new TermQuery(new Term(IndexFields.ITEM_PLACES, uri)), BooleanClause.Occur.MUST))
    
    val (docCollector, facetsCollector) = { 
      val dc =
        if (valueSource.isDefined) {
          // We're using sorting as defined in the value source
          val sort = new Sort(valueSource.get.getSortField(true)).rewrite(searcher)
          TopFieldCollector.create(sort, offset + limit, true, false, false)  
        } else {
          TopScoreDocCollector.create(offset + limit)
        }
      
      // Don't bother searching the taxo index if it's not requested
      if (includeFacets)
        (dc, Some(new FacetsCollector()))
      else
        (dc, None)
    }
    
    // Run the search
    if (facetsCollector.isDefined) {
      searcher.search(query, MultiCollector.wrap(docCollector, facetsCollector.get))
    } else {
      searcher.search(query, docCollector)
    }
    
    val topDocs = docCollector.topDocs()
    val facets = facetsCollector.map(fc => new FastTaxonomyFacetCounts(taxonomyReader, Index.facetsConfig, fc))
    
    (topDocs, facets)
  }

  private def executeSearch(query: BooleanQuery, places: Seq[String], limit: Int, offset: Int, searcher: IndexSearcher, taxonomyReader: DirectoryTaxonomyReader,
      valueSource: Option[ValueSource], includeFacets: Boolean, includeSnippets: Boolean): (Page[(IndexedObject, Option[String])], Option[FacetTree]) = {
    
    
    val (topDocs, facets) = 
      if (places.size > 0 && includeFacets) {
        // If there is a place filter, we need to drill sideways, rather than execute a standard search
        executeDrillSideways(query, places, limit, offset, searcher, taxonomyReader)
      } else {
        executeStandardQuery(query, places, limit, offset, searcher, taxonomyReader, valueSource, includeFacets)
      }
    
    val total = topDocs.totalHits

    // Compute facets, optionally
    val facetTree = facets.map(new FacetTree(_))      
    
    // Prepare snippet highlighter, optionally
    val highlighter =
      if (includeSnippets) {
        val previewFormatter = new SimpleHTMLFormatter("<strong>", "</strong>")
        val scorer = new QueryScorer(query)
        val highlighter = new Highlighter(previewFormatter, scorer)
        highlighter.setTextFragmenter(new SimpleFragmenter(PREVIEW_SNIPPET_SIZE))
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE)  
        Some(highlighter)
      } else {
        None
      }
       
    // Fetch result documents
    val results = topDocs.scoreDocs.drop(offset).map(scoreDoc => {      
      val document = searcher.doc(scoreDoc.doc)
 
      // Fetch snippets, optionally
      val previewSnippet = highlighter.flatMap(h => {
        Option(document.get(IndexFields.ITEM_FULLTEXT)).map(fulltext => {  
          val stream = TokenSources.getAnyTokenStream(searcher.getIndexReader, scoreDoc.doc, IndexFields.ITEM_FULLTEXT, analyzer)
          h.getBestFragments(stream, fulltext, PREVIEW_MAX_NUM_SNIPPETS, PREVIEW_SNIPPET_SEPARATOR)        
        })
      })
      
      (new IndexedObject(document), previewSnippet) 
    })
    
    (Page(results.toSeq, offset, limit, total), facetTree)
  }
  
  private def calculateTemporalProfile(filter: Filter, searcher: IndexSearcher): TimeHistogram = {
    val startCal = Index.dateRangeTree.newCal()
    startCal.set(-10000, Calendar.JANUARY, 1)
    val start = Index.dateRangeTree.toShape(startCal)
    
    val endCal = Index.dateRangeTree.newCal()
    endCal.set(10000, Calendar.DECEMBER, 31)
    val end = Index.dateRangeTree.toShape(endCal)
    
    val facetRange = Index.dateRangeTree.toRangeShape(start, end);    
    val tempFacets = Index.temporalStrategy.calcFacets(searcher.getTopReaderContext, filter, facetRange, 4)
    
    val values = tempFacets.parents.asScala.toSeq.map { case (shape, fpv) => {
      val calendar = Index.dateRangeTree.toObject(shape).asInstanceOf[Calendar]
      val year = calendar.get(Calendar.ERA) match {
        case GregorianCalendar.BC => - calendar.get(Calendar.YEAR)
        case _ => calendar.get(Calendar.YEAR)
      }
      
      val count = fpv.parentLeaves
      (year, count)
    }}

    TimeHistogram.create(values, 30)
  }
  
  private def calculateItemHeatmap(filter: Filter, bbox: Rectangle, level: Int, searcher: IndexSearcher): Heatmap = {
    val heatmap = HeatmapFacetCounter.calcFacets(Index.rptStrategy, searcher.getTopReaderContext, filter, bbox, level, 100000)
          
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
