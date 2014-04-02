/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import requests._

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
    val r = SetCensusControlRequest(request.body)
    if (r.errors.length > 0)
      BadRequest(r.errorsToJson)
    else {
      Async {
        OutRequests.ping map {
          response => Ok(Json.obj("status" -> "success"))
        } recover {
          case _ => InternalServerError(Json.obj("status" -> "unreachable host"))
        }
      }
    }
  }

  /** Route: GET /control */ 
  def getControl = Action {
    Ok(Json.obj(
      "host" -> OutRequests.host,
      "port" -> OutRequests.port
    ))
  }

  /** Route: POST /graph */ 
  def postGraph = Action(parse.json) { implicit request =>
    val r = GraphImportRequest(request.body)
    if (r.errors.length > 0)
      BadRequest(r.errorsToJson)
    else {
      RequestsQueue.enqueue(r)
      Ok(Json.obj("status" -> "acknowledged"))
    }
  }

  /** Route: POST /compute */ 
  def postCompute = Action(parse.json) { implicit request =>
    val r = ComputationRequest(request.body)
    if (r.errors.length > 0)
      BadRequest(r.errorsToJson)
    else {
      RequestsQueue.enqueue(r)
      Ok(Json.obj("status" -> "acknowledged"))
    }
  }

}
