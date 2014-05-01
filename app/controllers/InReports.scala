/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import com.github.nscala_time.time.Imports._ 

import play.api._
import play.api.mvc._
import play.api.libs.json._

import instances.Orchestrator

object InReports extends Controller {
  
  /** Route: GET /test */ 
  def test = Action { request => 
    Ok(request.remoteAddress)
  }
  
  /** Route: POST /censusengine/report */ 
  def report = Action(parse.json) { implicit request =>
    (request.body \ "token").asOpt[String] match {
      case Some(token) => Orchestrator.finished(request.remoteAddress, token)
      case None => println(s"${DateTime.now} - ERROR: Invalid Census Engine response, please check for bugs.")
    }
    Ok
  }

  /** Route: POST /censusengine/error */ 
  def error = Action(parse.json) { implicit request =>
    println(s"${DateTime.now} - ERROR: ${request.body}")
    Ok
  }

}
