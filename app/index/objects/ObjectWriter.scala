package index.objects

import index._
import models.core.{ Dataset, AnnotatedThing, Image }
import org.apache.lucene.index.{ IndexWriterConfig, Term }
import org.apache.lucene.search.{ BooleanQuery, BooleanClause, TermQuery }
import play.api.db.slick._
import index.places.IndexedPlaceNetwork

trait ObjectWriter extends IndexBase {
  
  def addAnnotatedThing(annotatedThing: AnnotatedThing, places: Seq[(IndexedPlaceNetwork, String)], images: Seq[Image], fulltext: Option[String], datasetHierarchy: Seq[Dataset])(implicit s: Session) =
    addAnnotatedThings(Seq((annotatedThing, places, images, fulltext)), datasetHierarchy)
  
  def addAnnotatedThings(annotatedThings: Seq[(AnnotatedThing, Seq[(IndexedPlaceNetwork, String)], Seq[Image], Option[String])], datasetHierarchy: Seq[Dataset])(implicit s: Session) =
    // NOTE: not sure parallelization is totally safe the way we're using it here
    annotatedThings.par.foreach { case (thing, places, images, fulltext) =>
      objectWriter.addDocument(Index.facetsConfig.build(taxonomyWriter, IndexedObject.toDoc(thing, places, images, fulltext, datasetHierarchy)))}
  
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
  
  /** Removes the dataset (and all items inside it) from the index **/
  def dropDataset(id: String) = {
    // Delete annotated things for this dataset
    objectWriter.deleteDocuments(new Term(IndexFields.SOURCE_DATASET, id))
      
    // Delete the dataset
    val q = new BooleanQuery()
    q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString)), BooleanClause.Occur.MUST)
    q.add(new TermQuery(new Term(IndexFields.ID, id)), BooleanClause.Occur.MUST)
    objectWriter.deleteDocuments(q)
  }

}
