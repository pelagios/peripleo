package controllers.common.harvest

import models.Datasets
import play.api.Play.current
import play.api.db.slick._
import play.api.Logger

class HarvestWorker {
	
  def harvest(datasetId: String) = {
    DB.withSession { implicit session: Session =>
      val d = Datasets.findById(datasetId)
      if (d.isEmpty) {
	    // TODO error notification
      } else {
	    val (dataset, dumpfiles) = d.get
 	    Logger.info("Harvesting " + dataset.title + ", " + dumpfiles.size + " dumpfiles") 
     }
    }
  }
	
}
