package index.annotations

import com.vividsolutions.jts.geom.Geometry
import index.Index
import models.core.{ Annotation, AnnotatedThing }

trait AnnotationWriter extends AnnotationReader {

  def addAnnotations(annotations: Seq[(AnnotatedThing, AnnotatedThing, Annotation, Geometry, Option[String], Option[String])]) =
    annotations.foreach { case (rootParent, parent, annotation, geometry, prefix, suffix) =>
      annotationWriter.addDocument(Index.facetsConfig.build(taxonomyWriter, IndexedAnnotation.toDoc(rootParent, parent, annotation, geometry, prefix, suffix))) }
  
}