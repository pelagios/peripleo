package index.annotations

import com.vividsolutions.jts.geom.Geometry
import index.Index
import models.core.{ Annotation, AnnotatedThing }

trait AnnotationWriter extends AnnotationReader {

  def addAnnotations(annotations: Seq[(AnnotatedThing, Annotation, Geometry, Option[String], Option[String])]) =
    annotations.foreach { case (topLevelParent, annotation, geometry, prefix, suffix) =>
      annotationWriter.addDocument(Index.facetsConfig.build(taxonomyWriter, IndexedAnnotation.toDoc(topLevelParent, annotation, geometry, prefix, suffix))) }
  
}