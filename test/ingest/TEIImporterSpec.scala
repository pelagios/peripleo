package ingest

import java.io.File
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.Play.current
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source
import models.core.Dataset
import java.sql.Date
import play.api.db.slick._

@RunWith(classOf[JUnitRunner])
class TEIImporterTest extends Specification  {
  
  val TEST_FILE = "test/resources/sample-short.tei.xml"

  "TEIImporter.importTEI" should {
    
    "not fail" in {
      running(FakeApplication()) {
        val teiFile = new File(TEST_FILE)

        val now = new Date(System.currentTimeMillis)
        val dataset = 
          Dataset("42", "Sample Dataset", "Sample Publisher", "CC-BY 3.0", 
            now, now, None, None, None, None, None, None, None, None)   
       
        DB.withSession { implicit session: Session =>
          TEImporter.importTEI(Source.fromFile(TEST_FILE, "UTF-8"), dataset)
        }
      
        (1) must equalTo (1)
      }
    }
    
  }
  
}
