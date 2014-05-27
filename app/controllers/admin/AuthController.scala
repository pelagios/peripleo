package controllers.admin

import play.api.Play
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{ Session => PlaySession, _ }
import play.api.db.slick._

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
      user => Redirect(controllers.routes.DatasetController.listAll()).withSession("username" -> user._1)
    )
  }
  
  def logout = Action {
    Redirect(routes.AuthController.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

}

trait Secured {
  
  private def username(request: RequestHeader) = request.session.get(Security.username)
  
  private def onUnauthorized(request: RequestHeader) = Results.Forbidden("Not Authorized.")

  def adminAction(f: => String => DBSessionRequest[AnyContent] => SimpleResult) = {
    Security.Authenticated(username, onUnauthorized) { username =>
      DBAction(BodyParsers.parse.anyContent)(rs => f(username)(rs))
    }
  }
  
}