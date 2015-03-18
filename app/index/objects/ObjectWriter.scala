package index.objects

import index._
import models.core.{ Dataset, AnnotatedThing }
import org.apache.lucene.index.{ IndexWriterConfig, Term }
import org.apache.lucene.search.{ BooleanQuery, BooleanClause, TermQuery }
import play.api.db.slick._
import index.places.IndexedPlace

trait ObjectWriter extends IndexBase {
  
  def addAnnotatedThing(annotatedThing: AnnotatedThing, places: Seq[IndexedPlace], fulltext: Option[String], datasetHierarchy: Seq[Dataset])(implicit s: Session) =
    addAnnotatedThings(Seq((annotatedThing, places, fulltext)), datasetHierarchy)
  
  def addAnnotatedThings(annotatedThings: Seq[(AnnotatedThing, Seq[IndexedPlace], Option[String])], datasetHierarchy: Seq[Dataset])(implicit s: Session) =
    // NOTE: not sure parallelization is totally safe the way we're using it here
    annotatedThings.par.foreach { case (thing, places, fulltext) =>
      objectWriter.addDocument(Index.facetsConfig.build(taxonomyWriter, IndexedObject.toDoc(thing, places, fulltext, datasetHierarchy)))}
  
  def addDataset(dataset: Dataset) = addDatasets(Seq(dataset))
  
  def addDatasets(datasets: Seq[Dataset]) =
    datasets.foreach(dataset =>
      objectWriter.addDocument(Index.facetsConfig.build(taxonomyWriter, IndexedObject.toDoc(dataset))))
  
  def updateDatasets(datasets: Seq[Dataset]) = {    
    // Delete
    datasets.foreach(dataset => {
      val q = new BooleanQuery()
      q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString)), BooleanClause.Occur.MUST)
      q.add(new TermQuery(new Term(IndexFields.ID, dataset.id)), BooleanClause.Occur.MUST)
      objectWriter.deleteDocuments(q)
    })
    
    // Add updated versions
    datasets.foreach(dataset =>
      objectWriter.addDocument(Index.facetsConfig.build(taxonomyWriter, IndexedObject.toDoc(dataset))))   
  }
  
  /** Removes datasets from the index. 
    *  
    * This method does NOT automatically take care of removing subsets - you need to 
    * hand the the whole dataset hierarchy
    */
  def dropDatasets(ids: Seq[String]) =
    ids.foreach(id => {
      // Delete annotated things for this dataset
      objectWriter.deleteDocuments(new Term(IndexFields.ITEM_DATASET, id))
      
      // Delete the dataset
      val q = new BooleanQuery()
      q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString)), BooleanClause.Occur.MUST)
      q.add(new TermQuery(new Term(IndexFields.ID, id)), BooleanClause.Occur.MUST)
      objectWriter.deleteDocuments(q)
    })

}
