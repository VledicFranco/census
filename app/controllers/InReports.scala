/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import scala.collection.mutable.HashMap

import com.github.nscala_time.time.Imports._ 

import play.api._
import play.api.mvc._
import play.api.libs.json._

import instances.Instance

object InReports extends Controller {

  private val listeners: HashMap[Instances] = HashMap()

  def register (listener: Instance): Unit = {
    listeners += (listener.host -> listener)
  }

  def unregister (listener: Instance): Unit = {
    listeners -= listener.host
  }
  
  /** Route: GET /test */ 
  def test = Action { request => 
    Ok(request.remoteAddress)
  }
  
  /** Route: POST /censusengine/report */ 
  def report = Action(parse.json) { implicit request =>
    val token = (request.body \ "token").as[String]
    val host = request.remoteAddress
    val listener = listeners.apply(host)
    listener.report(token)
    Ok
  }

  /** Route: POST /censusengine/error */ 
  def error = Action(parse.json) { implicit request =>
    val token = (request.body \ "token").as[String]
    val error = (request.body \ "error").as[String]
    val on = (request.body \ "on").as[String]
    val host = request.remoteAddress
    val listener = listeners.apply(host)
    listener.error(token, error, on)
    Ok
  }

}
