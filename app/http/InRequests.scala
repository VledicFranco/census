/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control.http

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import control.requests.ControlComputeRequest
import controllers.requests.{SetOutReportsRequest, EngineImportRequest, EngineComputeRequest}

import control.Orchestrator
import control.Instance

/**
 * Module that handles the Play Framework HTTP main Census Control
 * requests.
 */
object InRequests extends Controller {
  
  /** Route: GET / */ 
  def index = Action {
    Ok("Hello I am Census, how can I serve you?")
  }

  /** Route: GET /test */
  def test = Action {
    Ok("Test init.")
  }

  /** Route: POST /reports */ 
  def reportsservice = Action(parse.json) { implicit request =>
    val r = SetOutReportsRequest(request.body)
    if (r.hasValidationErrors)
      BadRequest(r.errorsToJson)
    else
      Ok(Json.obj("status" -> "success"))
  }

  /** Route: POST /control/compute */ 
  def controlcompute = Action(parse.json) { implicit request =>
    val r = ControlComputeRequest(request.body)
    if (r.hasErrors)
      BadRequest(r.errorsToJson)
    else {
      Ok(Json.obj(
        "status" -> "acknowledged",
        "token" -> r.token
      ))
    }
  }
  
  /** Route: POST /engine/import */ 
  def engineimport = Action(parse.json) { implicit request =>
    val r = EngineImportRequest(request.body)
    if (r.hasValidationErrors)
      BadRequest(r.errorsToJson)
    else {
      RequestsQueue.enqueue(r)
      Ok(Json.obj("status" -> "acknowledged"))
    }
  }

  /** Route: POST /engine/compute */ 
  def enginecompute = Action(parse.json) { implicit request =>
    val r = EngineComputeRequest(request.body)
    if (r.hasValidationErrors)
      BadRequest(r.errorsToJson)
    else {
      RequestsQueue.enqueue(r)
      Ok(Json.obj("status" -> "acknowledged"))
    }
  }

}
