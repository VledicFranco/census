/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control.http

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import requests.ControlComputeRequest
import requests.EngineComputeRequest
import requests.EngineImportRequest
import requests.SetOutReportsRequest

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
    val req = new SetOutReportsRequest(request.body)
    if (req.hasErrors)
      BadRequest(req.errorsToJson)
    else
      Ok(Json.obj("status" -> "success"))
  }

  /** Route: POST /control/compute */ 
  def controlcompute = Action(parse.json) { implicit request =>
    val req = new ControlComputeRequest(request.body)
    if (req.hasErrors)
      BadRequest(req.errorsToJson)
    else
      Ok(Json.obj(
        "status" -> "acknowledged",
        "token" -> req.token
      ))
  }

  /** Route: POST /engine/import */ 
  def engineimport = Action(parse.json) { implicit request =>
    val req = EngineImportRequest(request.body)
    if (req.hasErrors)
      BadRequest(req.errorsToJson)
    else
      Ok(Json.obj("status" -> "acknowledged"))
  }

  /** Route: POST /engine/compute */ 
  def enginecompute = Action(parse.json) { implicit request =>
    val r = EngineComputeRequest(request.body)
    if (r.hasValidationErrors)
      BadRequest(r.errorsToJson)
    else
      Ok(Json.obj("status" -> "acknowledged"))
  }

}
