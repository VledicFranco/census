/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import controllers.requests.{SetOutReportsRequest, EngineImportRequest, EngineComputeRequest}

import com.signalcollect._

/**
 * Module that handles the Play Framework http
 * requests.
 */
object InRequests extends Controller {
  
  /** Route: GET / */ 
  def index = Action {
    Ok("Hello, I am Census 2 Engine, how can I serve you?")
  }

  /** Route: POST /control */ 
  def postControl = Action(parse.json) { implicit request =>
    val r = SetOutReportsRequest(request.body)
    if (r.hasValidationErrors)
      BadRequest(r.errorsToJson)
    else
      Ok(Json.obj("status" -> "success"))
  }

  /** Route: GET /control */ 
  def getControl = Action {
    Ok(Json.obj(
      "host" -> OutReports.host,
      "port" -> OutReports.port
    ))
  }

  /** Route: POST /graph */ 
  def postGraph = Action(parse.json) { implicit request =>
    val r = EngineImportRequest(request.body)
    if (r.hasValidationErrors)
      BadRequest(r.errorsToJson)
    else {
      RequestsQueue.enqueue(r)
      Ok(Json.obj("status" -> "acknowledged"))
    }
  }

  /** Route: POST /compute */ 
  def postCompute = Action(parse.json) { implicit request =>
    val r = EngineComputeRequest(request.body)
    if (r.hasValidationErrors)
      BadRequest(r.errorsToJson)
    else {
      RequestsQueue.enqueue(r)
      Ok(Json.obj("status" -> "acknowledged"))
    }
  }

}
