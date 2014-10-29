package index.objects

import index._
import models.{ Dataset, AnnotatedThing }
import org.apache.lucene.index.{ IndexWriterConfig, Term }
import org.apache.lucene.search.{ BooleanQuery, BooleanClause, TermQuery }
import play.api.db.slick._
import index.places.IndexedPlace

trait ObjectWriter extends IndexBase {
  
  def addAnnotatedThing(annotatedThing: AnnotatedThing, places: Seq[IndexedPlace], datasetHierarchy: Seq[Dataset])(implicit s: Session) =
    addAnnotatedThings(Seq((annotatedThing, places)), datasetHierarchy)
  
  def addAnnotatedThings(annotatedThings: Seq[(AnnotatedThing, Seq[IndexedPlace])], datasetHierarchy: Seq[Dataset])(implicit s: Session) = {
    val (indexWriter, taxonomyWriter) = newObjectWriter() 
    
    // NOTE: not sure parallelization is totally safe the way we're using it here
    annotatedThings.par.foreach { case (thing, places) =>
      indexWriter.addDocument(facetsConfig.build(taxonomyWriter, IndexedObject.toDoc(thing, places, datasetHierarchy)))}
    
    indexWriter.close()
    taxonomyWriter.close()   
  }
  
  def addDataset(dataset: Dataset) = addDatasets(Seq(dataset))
  
  def addDatasets(datasets: Seq[Dataset]) = { 
    val (indexWriter, taxonomyWriter) = newObjectWriter() 
    
    datasets.foreach(dataset =>
      indexWriter.addDocument(facetsConfig.build(taxonomyWriter, IndexedObject.toDoc(dataset))))
    
    indexWriter.close()
    taxonomyWriter.close()    
  }
  
  /** Removes datasets from the index. 
    *  
    * This method does NOT automatically take care of removing subsets - you need to 
    * hand the the whole dataset hierarchy
    */
  def dropDatasets(ids: Seq[String]) = {
    val (indexWriter, taxonomyWriter) = newObjectWriter()
    
    // Delete annotated things - it's enough to purge everything that has a 'dataset' field == id
    val deleteThingsQuery = new BooleanQuery()
    ids.foreach(id =>
      deleteThingsQuery.add(new TermQuery(new Term(IndexFields.DATASET, id)), BooleanClause.Occur.SHOULD))   
    indexWriter.deleteDocuments(deleteThingsQuery)
    
    // Delete dataset
    val deleteDatasetsQuery = new BooleanQuery()
    deleteDatasetsQuery.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString)), BooleanClause.Occur.MUST)
    ids.foreach(id =>
      deleteDatasetsQuery.add(new TermQuery(new Term(IndexFields.ID, id)), BooleanClause.Occur.SHOULD))
    indexWriter.deleteDocuments(deleteDatasetsQuery)
    
    indexWriter.close()
    taxonomyWriter.close()
  }

}
