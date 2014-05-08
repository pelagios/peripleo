package controllers.admin

import play.api.mvc.Controller

object DatasetAdminController extends Controller with Secured {

  def index = adminAction { username => implicit requestWithSession =>
    Ok(views.html.admin.datasets())
  }
  
  def uploadDataset = adminAction { username => implicit requestWithSession =>
    Ok("Ok.")
  }
  
}