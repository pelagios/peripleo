package index.annotations

import com.vividsolutions.jts.geom.Geometry
import index.Index
import models.core.Annotation

trait AnnotationWriter extends AnnotationReader {

  def addAnnotations(annotations: Seq[(Annotation, Option[Int], Option[Int], Geometry, Option[String], Option[String])]) =
    annotations.foreach { case (annotation, tempBoundsStart, tempBoundsEnd, geometry, prefix, suffix) =>
      annotationWriter.addDocument(Index.facetsConfig.build(taxonomyWriter, IndexedAnnotation.toDoc(annotation, tempBoundsStart, tempBoundsEnd, geometry, prefix, suffix))) }
  
}