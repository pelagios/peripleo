package index.objects

import index._
import models.{ Dataset, AnnotatedThing }
import org.apache.lucene.index.{ IndexWriterConfig, Term }
import org.apache.lucene.search.{ BooleanQuery, BooleanClause, TermQuery }
import play.api.db.slick._

trait ObjectWriter extends IndexBase {
  
  def addAnnotatedThing(annotatedThing: AnnotatedThing)(implicit s: Session) =
    addAnnotatedThings(Seq(annotatedThing))
  
  def addAnnotatedThings(annotatedThings: Seq[AnnotatedThing])(implicit s: Session) = {
    val (indexWriter, taxonomyWriter) = newObjectWriter() 
    
    annotatedThings.foreach(thing =>
      indexWriter.addDocument(facetsConfig.build(taxonomyWriter, IndexedObject.toDoc(thing))))
    
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
