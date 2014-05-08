package controllers.admin

import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{ Controller, Action }

object AuthController extends Controller {
  
  val adminUser = Play.current.configuration.getString("admin.user").getOrElse("admin")
  val adminPassword = Play.current.configuration.getString("admin.password").getOrElse("admin")
  
  val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (username, password) => {
        username.equals(adminUser) & password.equals(adminPassword)
      }
    })
  )
  
  def login = Action { implicit request =>
    Ok(views.html.admin.login(loginForm))
  }
  
  def authenticate = Action { implicit request => 
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.admin.login(formWithErrors)),
      user => Redirect(controllers.routes.DatasetController.listAll).withSession("username" -> user._1)
    )
  }
  
  def logout = Action {
    Redirect(routes.AuthController.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

}