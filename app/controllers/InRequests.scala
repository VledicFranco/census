/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import requests._

import instances.Orchestrator
import instances.Instance
import instances.GCE

/**
 * Module that handles the Play Framework http
 * requests.
 */
object InRequests extends Controller {
  
  /** Route: GET / */ 
  def index = Action {
    Ok("Hello, I am Census 2 Control, how can I serve you?")
  }

  /** Route: GET /test */
  def test = Action {
    //GCE.verifyToken
//    val json = Json.obj(
//      "arr" -> Json.arr(Json.obj(
//        "some" -> "value"
//      ))
//    )
//    for (a <- (json \ "arr" \\ "some")) {
//      println(a.as[String])
//    }
    GCE.createInstance { instance =>
      println(s"WOOT!! :: ${instance.host}")
    }
    Ok("Test init.")
  }

  /** Route: POST /hook */ 
  def postHTTPHook = Action(parse.json) { implicit request =>
    val r = SetHTTPHookRequest(request.body)
    if (r.errors.length > 0)
      BadRequest(r.errorsToJson)
    else {
      Async {
        HTTPHook.ping map {
          response => Ok(Json.obj("status" -> "success"))
        } recover {
          case _ => InternalServerError(Json.obj("status" -> "unreachable host"))
        }
      }
    }
  }

  /** Route: GET /hook */ 
  def getHTTPHook = Action {
    Ok(Json.obj(
      "host" -> HTTPHook.host,
      "port" -> HTTPHook.port
    ))
  }

  /** Route: POST /compute */ 
  def postCompute = Action(parse.json) { implicit request =>
    val r = ComputationRequest(request.body)
    if (r.errors.length > 0)
      BadRequest(r.errorsToJson)
    else {
//      RequestsQueue.enqueue(r)
      Ok(Json.obj(
        "status" -> "acknowledged",
        "token" -> r.algorithm.token
      ))
    }
  }
  
}
