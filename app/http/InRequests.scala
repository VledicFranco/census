/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control.http

import scala.concurrent._

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
import http.OutReports

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
      return BadRequest(req.errorsToJson)

    OutReports.setService(req.host, req.port)
    Ok(Json.obj("status" -> "success"))
  }

  /** Route: POST /control/compute */ 
  def controlcompute = Action(parse.json) { implicit request =>
    val req = new ControlComputeRequest(request.body)
    if (req.hasErrors) 
      return BadRequest(req.errorsToJson)

    database.ping { success => 
      if (!success) return OutReports.Error.unreachableNeo4j(this)
      algorithm.receive
    } 
    Ok(Json.obj(
      "status" -> "acknowledged",
      "token" -> req.token
    ))
  }

  /** Route: POST /engine/import */ 
  def engineimport = Action(parse.json) { implicit request =>
    val req = EngineImportRequest(request.body)
    if (req.hasErrors)
      return BadRequest(req.errorsToJson)

    DB.tag = req.tag
    DB.setDatabase(req.host, req.port, req.user, req.password)
    DB.ping { success =>  
      if (!success) return OutReports.Error.unreachableNeo4j(req)
      if (DB.importedGraphFormat != null) 
        DB.importedGraphFormat.clear
      DB.importedGraphFormat = req.graph
      req.graph.importStart(req)
    }
    Ok(Json.obj("status" -> "acknowledged"))
  }

  /** Route: POST /engine/compute */ 
  def enginecompute = Action(parse.json) { implicit request =>
    val req = EngineComputeRequest(request.body)
    if (req.hasValidationErrors)
      return BadRequest(req.errorsToJson)

    future { req.algorithm.computeStart(req) }
    Ok(Json.obj("status" -> "acknowledged"))
  }

}
