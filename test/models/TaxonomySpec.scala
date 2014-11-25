package models

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import models.geo._

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class TaxonomySpec extends Specification  {
  
  "Taxonomy" should {
    
    "return correct path for a set of tags" in {
      val tags = Seq("Inscription", "Scholarship")
      val paths = Taxonomy.getPaths(tags)
      
      println(paths.toString)
      
      paths must contain(===(Seq("Texts", "Inscriptions")))
      paths must contain(===(Seq("Scholarship"))) 
    }
    
  }

}