package index.annotations

import com.vividsolutions.jts.geom.Geometry
import index.Index
import index.places.IndexedPlaceNetwork
import models.core.{ Annotation, AnnotatedThing }

trait AnnotationWriter extends AnnotationReader {

  def addAnnotations(annotations: Seq[(AnnotatedThing, AnnotatedThing, Annotation, IndexedPlaceNetwork, Option[String], Option[String])]) =
    annotations.foreach { case (rootParent, parent, annotation, place, prefix, suffix) =>
      annotationWriter.addDocument(Index.facetsConfig.build(taxonomyWriter, IndexedAnnotation.toDoc(rootParent, parent, annotation, place, prefix, suffix))) }
  
}