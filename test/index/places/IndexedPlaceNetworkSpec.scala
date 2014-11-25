package index.places

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class IndexedPlaceNetworkSpec extends Specification  {
  
  private def createTestPlace(uri: String, label: String, closeMatch: String) = {
    new IndexedPlace("{\"uri\":\"" + uri + "\", \"label\":\"" + label + "\", \"source_gazetteer\":\"none\", \"names\":[], \"close_matches\":[\"" + closeMatch + "\"], \"exact_matches\":[] }")
  }
  
  // The test network looks like this [DAI]-->(geonames)<--[DARE]<--[VICI]
  // If we remove DARE, the network falls apart into two parts (containing [VICI], and [DAI], respectively)
  val testPlaces = Seq(
    createTestPlace("http://vici.org/vici/2725", "Colonia Vienna", "http://www.imperium.ahlfeldt.se/places/106"),
    createTestPlace("http://www.imperium.ahlfeldt.se/places/106", "Vienne", "http://sws.geonames.org/2969284"),
    createTestPlace("http://gazetteer.dainst.org/place/2080717", "Vienne", "http://sws.geonames.org/2969284")
  )
  
  "IndexedPlaceNetwork.buildNetworks" should {
    
    "build a single network from the test data" in {
      val networks = IndexedPlaceNetwork.buildNetworks(testPlaces)
      (networks.size) must equalTo (1)
    }
    
    "build two networks if DARE is removed from the test data" in {
      val placesWithoutDARE = testPlaces.filter(!_.uri.startsWith("http://www.imperium.ahlfeldt.se"))
      val networks = IndexedPlaceNetwork.buildNetworks(placesWithoutDARE)
      (networks.size) must equalTo (2)
    }
    
  }

}