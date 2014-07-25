/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package http

import scala.concurrent._

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import requests.ControlComputeRequest
import requests.EngineComputeRequest
import requests.EngineImportRequest
import requests.SetOutReportsRequest

import control.QueueAllSources
import control.QueueSinglet
import control.Orchestrator
import control.Instance

import engine.DB

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
    else {
      OutReports.setService(req.host, req.port)
      Ok(Json.obj("status" -> "success"))
    }
  }

  /** Route: POST /control/compute */ 
  def controlcompute = Action(parse.json) { implicit request =>
    val req = new ControlComputeRequest(request.body)
    if (req.hasErrors) 
      BadRequest(req.errorsToJson)
    else {
      req.database.ping { success => 
        if (!success) 
          OutReports.Error.unreachableNeo4j(req)
        else 
          req.bulk match {
            case "singlet" => new Orchestrator(req) with QueueSinglet
            case "all-sources" => new Orchestrator(req) with QueueAllSources
          }
      } 
      Ok(Json.obj(
        "status" -> "acknowledged",
        "token" -> req.token
      ))
    }
  }

  /** Route: POST /engine/import */ 
  def engineimport = Action(parse.json) { implicit request =>
    val req = new EngineImportRequest(request.body)
    if (req.hasErrors)
      BadRequest(req.errorsToJson)
    else {
      DB.tag = req.tag
      DB.setDatabase(req.host, req.port, req.user, req.password)
      DB.ping { success =>  
        if (!success) 
          OutReports.Error.unreachableNeo4j(req)
        else {
          if (DB.importedGraphFormat != null) 
            DB.importedGraphFormat.clear
          DB.importedGraphFormat = req.graph
          req.graph.importStart(req)
        }
      }
      Ok(Json.obj("status" -> "acknowledged"))
    }
  }

  /** Route: POST /engine/compute */ 
  def enginecompute = Action(parse.json) { implicit request =>
    val req = new EngineComputeRequest(request.body)
    if (req.hasErrors)
      BadRequest(req.errorsToJson)
    else {
      future { req.algorithm.computeStart(req, req.vars) }
      Ok(Json.obj("status" -> "acknowledged"))
    }
  }

}
