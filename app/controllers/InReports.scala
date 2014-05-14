/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import scala.collection.mutable.ArrayBuffer

import com.github.nscala_time.time.Imports._ 

import play.api._
import play.api.mvc._
import play.api.libs.json._

object InReports extends Controller {

  private val listeners: ArrayBuffer[InReportsListener] = ArrayBuffer()

  def register (listener: InReportsListener): Unit = {
    listeners += listener
  }

  def unregister (listener: InReportsListener): Unit = {
    listeners -= listener
  }
  
  /** Route: GET /test */ 
  def test = Action { request => 
    Ok(request.remoteAddress)
  }
  
  /** Route: POST /censusengine/report */ 
  def report = Action(parse.json) { implicit request =>
    val token = (request.body \ "token").as[String]
    for (listener <- listeners) {
      println(s"Report from ${request.remoteAddress}: $token")
      listener.report(request.remoteAddress, token)
    }
    Ok
  }

  /** Route: POST /censusengine/error */ 
  def error = Action(parse.json) { implicit request =>
    println(s"${DateTime.now} - ERROR: ${request.body}")
    Ok
  }

}
