package index.objects

import com.spatial4j.core.shape.Rectangle
import com.spatial4j.core.distance.DistanceUtils
import com.vividsolutions.jts.geom.Coordinate
import index._
import index.DateFilterMode._
import index.annotations.AnnotationReader
import index.places.IndexedPlaceNetwork
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
import org.apache.lucene.queryparser.classic.{ MultiFieldQueryParser, QueryParser }
import org.apache.lucene.spatial.prefix.HeatmapFacetCounter
import org.apache.lucene.spatial.prefix.tree.NumberRangePrefixTree.UnitNRShape
import org.apache.lucene.spatial.query.{ SpatialArgs, SpatialOperation }
import play.api.db.slick._
import scala.collection.JavaConverters._

trait ObjectReader extends AnnotationReader {
  
  private val PREVIEW_SNIPPET_SIZE = 120
  
  private val PREVIEW_MAX_NUM_SNIPPETS = 3
  
  private val PREVIEW_SNIPPET_SEPARATOR = " ... "
  
  /** Helper function that adds time query condition if defined **/
  private def addTimeFilter(query: BooleanQuery, from: Option[Int], to: Option[Int], dateFilterMode: DateFilterMode.Value) = {
    if (from.isDefined || to.isDefined) {
      // Open intervals are allowed
      val start = from match {
        case Some(start) => start
        case None => Integer.MIN_VALUE
      }
    
      val end = to match {
        case Some(end) => end
        case None => Integer.MAX_VALUE
      }
      
      val dateRange =
        if (start > end) // Just a safety precaution... 
          Index.dateRangeTree.parseShape("[" + end + " TO " + start + "]")
        else
          Index.dateRangeTree.parseShape("[" + start + " TO " + end + "]")

      dateFilterMode match {
        case DateFilterMode.INTERSECTS =>
          query.add(Index.temporalStrategy.makeQuery(new SpatialArgs(SpatialOperation.Intersects, dateRange)), BooleanClause.Occur.MUST)
      
        case DateFilterMode.CONTAINS =>
          query.add(Index.temporalStrategy.makeQuery(new SpatialArgs(SpatialOperation.IsWithin, dateRange)), BooleanClause.Occur.MUST)        
      }
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
      includeTimeHistogram: Boolean, includeTopPlaces: Int, includeHeatmap: Boolean)(implicit s: Session): 
      (Page[(IndexedObject, Option[String])], Option[FacetTree], Option[TimeHistogram], Option[Seq[(IndexedPlaceNetwork, Int)]], Option[Heatmap]) = {

    
    val rectangle = params.bbox.map(b => Index.spatialCtx.makeRectangle(b.minLon, b.maxLon, b.minLat, b.maxLat))
    
    // The base query is the part of the query that is the same for search, time histogram and heatmap calculation
    val (baseQuery, valueSource) = prepareBaseQuery(
      params.objectTypes, params.excludeObjectTypes, params.datasets, params.excludeDatasets,
      params.gazetteers, params.excludeGazetteers, params.places, rectangle, params.coord, params.radius)
      
    // Finalize search query and time histogram filter
    val (searchQuery, timeHistogramFilter) = {
      
      // In all cases, we want to include fulltext search...
      val baseSearchQuery = baseQuery.clone()
      if (params.query.isDefined) {
        val fields = Seq(
            IndexFields.TITLE, 
            IndexFields.DESCRIPTION,
            IndexFields.PLACE_NAME,
            IndexFields.ITEM_FULLTEXT,
            IndexFields.ANNOTATION_QUOTE,
            IndexFields.ANNOTATION_FULLTEXT_PREFIX,
            IndexFields.ANNOTATION_FULLTEXT_SUFFIX).toArray
            
        val parser = new MultiFieldQueryParser(fields, analyzer)
        parser.setPhraseSlop(0)
        parser.setDefaultOperator(QueryParser.Operator.AND)
        parser.setAutoGeneratePhraseQueries(true)
        baseSearchQuery.add(parser.parse(params.query.get), BooleanClause.Occur.MUST)  
      }
      
      // ...but we only want to restrict the SEARCH by time interval - the histogram should count all the facets
      val timeHistogramFilter = 
        if (includeTimeHistogram)
          Some(new QueryWrapperFilter(baseSearchQuery.clone()))
        else
          None
          
      addTimeFilter(baseSearchQuery, params.from, params.to, params.dateFilterMode)

      (baseSearchQuery, timeHistogramFilter)
    }
    
    // Finalize the heatmap filter (we don't search item fulltext, but want the time filter)
    val heatmapFilter = {
      if (includeHeatmap) {
        val h = baseQuery.clone()
      
        addTimeFilter(h, params.from, params.to, params.dateFilterMode)
      
        if (params.query.isDefined) { 
          val fields = Seq(IndexFields.TITLE, IndexFields.DESCRIPTION, IndexFields.PLACE_NAME).toArray   
          val parser = new MultiFieldQueryParser(fields, analyzer)
          parser.setAutoGeneratePhraseQueries(true)
          h.add(parser.parse(params.query.get), BooleanClause.Occur.MUST)  
        }
      
        Some(new QueryWrapperFilter(h))
      } else {
        None
      }
    }
  
    val placeSearcher = placeSearcherManager.acquire()
    val objectSearcher = objectSearcherManager.acquire()
    val annotationSearcher = annotationSearcherManager.acquire()
    
    // TODO we could optimize this a bit by searching only one index if filter are set accordingly
    val objectAndPlaceSearcher = new IndexSearcher(new MultiReader(objectSearcher.searcher.getIndexReader, placeSearcher.searcher.getIndexReader))
    val annotationAndPlaceSearcher = new IndexSearcher(new MultiReader(annotationSearcher.searcher.getIndexReader, placeSearcher.searcher.getIndexReader))
    
    try {   
      // Search & facet counts
      val (results, facets) = 
        executeSearch(searchQuery, params.query, params.places, params.limit, params.offset, objectAndPlaceSearcher, objectSearcher.taxonomyReader,
          valueSource, includeFacets, includeSnippets, annotationSearcher.searcher)
      
      // Top places
      val topPlaces = 
        if (includeTopPlaces > 0) {
          Some(calculateTopPlaces(searchQuery, includeTopPlaces, annotationAndPlaceSearcher, annotationSearcher.taxonomyReader))
        } else {
          None
        }
          
      // Time histogram computation
      val temporalProfile = timeHistogramFilter.map(filter => calculateTemporalProfile(filter, objectAndPlaceSearcher))
      
      // Heatmap computation
      // TODO fix! do we really need item heatmap any more? should it be annotationAndPlaceSearcher instead?
      val heatmap = heatmapFilter.map(filter => {
        val rect = rectangle.getOrElse(Index.spatialCtx.makeRectangle(-90, 90, -90, 90))
        val level = getHeatmapLevelForRect(rect)
        
        if (params.query.isDefined) {
          // If there is a query phrase, we include the annotation heatmap 
          calculateItemHeatmap(filter, rect, level, objectAndPlaceSearcher) +
          calculateAnnotationHeatmap(params.query, params.datasets, params.excludeDatasets, params.from, params.to, params.places, rectangle,
            params.coord, params.radius, level, annotationSearcher)
        } else {
          // Otherwise, we only need the item-based heatmap
          calculateItemHeatmap(filter, rect, level, objectAndPlaceSearcher)
        }
      })
      
      (results, facets, temporalProfile, topPlaces, heatmap)
    } finally {
      placeSearcherManager.release(placeSearcher)
      objectSearcherManager.release(objectSearcher)
      annotationSearcherManager.release(annotationSearcher)
    }
  }
  
  /** Constructs the query as far as it's common for search and heatmap computation **/
  private def prepareBaseQuery(
      objectTypes:        Seq[IndexedObjectTypes.Value],
      excludeObjectTypes: Seq[IndexedObjectTypes.Value],
      datasets:           Seq[String],
      excludeDatasets:    Seq[String],
      gazetteers:         Seq[String],
      excludeGazetteers:  Seq[String],
      places:             Seq[String], 
      bbox:               Option[Rectangle],
      coord:              Option[Coordinate], 
      radius:             Option[Double])(implicit s: Session): (BooleanQuery, Option[ValueSource]) = {
    
    val q = new BooleanQuery()
      
    // Object type filter
    if (objectTypes.size > 0) {
      if (objectTypes.size == 1) {
        q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, objectTypes.head.toString)), BooleanClause.Occur.MUST)
      } else {
        val typeQuery = new BooleanQuery()
        objectTypes.foreach(objectType => 
          q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, objectType.toString)), BooleanClause.Occur.SHOULD))
        q.add(typeQuery, BooleanClause.Occur.MUST)
      }
    } else if (excludeObjectTypes.size > 0) {
      excludeObjectTypes.foreach(objectType =>
        q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, objectType.toString)), BooleanClause.Occur.MUST_NOT))
    }
    
    // Source (dataset/gazetteer) filter
    if (datasets.size > 0 || gazetteers.size > 0) {
      val datasetsWithSubsets = datasets ++ datasets.flatMap(Datasets.listSubsetsRecursive(_))
      val allSourceIDs = datasetsWithSubsets ++ gazetteers.map(_.toLowerCase)
      
      if (allSourceIDs.size == 1) {
        q.add(new TermQuery(new Term(IndexFields.SOURCE_DATASET, allSourceIDs.head)), BooleanClause.Occur.MUST)        
      } else {
        val sourceQuery = new BooleanQuery()
        allSourceIDs.foreach(id =>
          sourceQuery.add(new TermQuery(new Term(IndexFields.SOURCE_DATASET, id)), BooleanClause.Occur.SHOULD))
        q.add(sourceQuery, BooleanClause.Occur.MUST)
      }
    } else if (excludeDatasets.size > 0 || excludeGazetteers.size > 0) {
      val datasetsWithSubsets = excludeDatasets ++ excludeDatasets.flatMap(Datasets.listSubsetsRecursive(_))
      val allExcludeIDs = datasetsWithSubsets ++ excludeGazetteers.map(_.toLowerCase)
      
      allExcludeIDs.foreach(id =>
        q.add(new TermQuery(new Term(IndexFields.SOURCE_DATASET, id)), BooleanClause.Occur.MUST_NOT))
    }
    
    // Places filter
    places.foreach(uri =>
      q.add(new TermQuery(new Term(IndexFields.PLACE_URI, uri)), BooleanClause.Occur.MUST))
      
    // Spatial filter
    val valuesource = {
      if (bbox.isDefined) {
        q.add(Index.bboxStrategy.makeQuery(new SpatialArgs(SpatialOperation.BBoxWithin, bbox.get)), BooleanClause.Occur.MUST)
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
  
  private def executeStandardQuery(query: BooleanQuery, places: Seq[String], limit: Int, offset: Int, searcher: IndexSearcher, 
      taxonomyReader: DirectoryTaxonomyReader, valueSource: Option[ValueSource], includeFacets: Boolean): (TopDocs, Option[Facets]) = {
    
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

  private def executeSearch(query: BooleanQuery, phrase: Option[String], places: Seq[String], limit: Int, offset: Int, searcher: IndexSearcher, taxonomyReader: DirectoryTaxonomyReader,
      valueSource: Option[ValueSource], includeFacets: Boolean, includeSnippets: Boolean, snippetSearcher: IndexSearcher): (Page[(IndexedObject, Option[String])], Option[FacetTree]) = {
    
    val (topDocs, facets) =
      executeStandardQuery(query, places, limit, offset, searcher, taxonomyReader, valueSource, includeFacets)
    
    val total = topDocs.totalHits

    // Compute facets, optionally
    val facetTree = facets.map(new FacetTree(_))      
    
    /* Prepare snippet highlighter, optionally
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
      */
       
    // Fetch result documents
    val results = topDocs.scoreDocs.drop(offset).map(scoreDoc => {      
      val document = searcher.doc(scoreDoc.doc)
 
      // Fetch snippets, optionally
      /*
      val previewSnippet = highlighter.flatMap(h => {
        Option(document.get(IndexFields.ITEM_FULLTEXT)).map(fulltext => {  
          val stream = TokenSources.getAnyTokenStream(searcher.getIndexReader, scoreDoc.doc, IndexFields.ITEM_FULLTEXT, analyzer)
          h.getBestFragments(stream, fulltext, PREVIEW_MAX_NUM_SNIPPETS, PREVIEW_SNIPPET_SEPARATOR)        
        })
      })
      */
      
      /** HACK **/
      
      val previewSnippet = 
        if (includeSnippets && phrase.isDefined) {
          
          // if document hasFulltext
          
          Option(document.get(IndexFields.ID)) match {
            
            case Some(id) => {
              val snippets = getSnippets(id, phrase.get, places.headOption, 3, snippetSearcher)
              Some(snippets.map("<p class=\"snippet\">..." + _ + "...</p>").mkString(""))
            }
            
            case None => None
          }
          
          
        } else {
          None
        }
      
      /** /HACK **/
      
      (new IndexedObject(document), previewSnippet) 
    })
    
    (Page(results.toSeq, offset, limit, total), facetTree)
  }
  
  private def calculateTemporalProfile(filter: Filter, searcher: IndexSearcher): TimeHistogram = {        
    val startCal = Index.dateRangeTree.newCal()
    startCal.set(-8000, Calendar.JANUARY, 1)
    val start = Index.dateRangeTree.toShape(startCal)
    
    val endCal = Index.dateRangeTree.newCal()
    endCal.set(3000, Calendar.DECEMBER, 31)
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

    TimeHistogram.create(values, 35)
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
