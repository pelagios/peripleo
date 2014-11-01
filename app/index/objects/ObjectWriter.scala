package index.objects

import index._
import models.core.{ Dataset, AnnotatedThing }
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
  
  def updateDatasets(datasets: Seq[Dataset]) = {
    val (indexWriter, taxonomyWriter) = newObjectWriter() 
    
    // Delete
    datasets.foreach(dataset => {
      val q = new BooleanQuery()
      q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString)), BooleanClause.Occur.MUST)
      q.add(new TermQuery(new Term(IndexFields.ID, dataset.id)), BooleanClause.Occur.MUST)
      indexWriter.deleteDocuments(q)
    })
    
    // Add updated versions
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
    
    ids.foreach(id => {
      // Delete annotated things for this dataset
      indexWriter.deleteDocuments(new Term(IndexFields.ITEM_DATASET, id))
      
      // Delete the dataset
      val q = new BooleanQuery()
      q.add(new TermQuery(new Term(IndexFields.OBJECT_TYPE, IndexedObjectTypes.DATASET.toString)), BooleanClause.Occur.MUST)
      q.add(new TermQuery(new Term(IndexFields.ID, id)), BooleanClause.Occur.MUST)
      indexWriter.deleteDocuments(q)
    })
    
    indexWriter.close()
    taxonomyWriter.close()
  }

}
