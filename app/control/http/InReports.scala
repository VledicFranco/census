/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control.http

import scala.collection.mutable.HashMap

import com.github.nscala_time.time.Imports._ 

import play.api._
import play.api.mvc._
import play.api.libs.json._

import instances.Instance

/**
 * Module that receives reports from the Census Engine instances.
 */
object InReports extends Controller {

  /** Map with all the Census Engine instances. */
  private val listeners: HashMap[String, Instance] = HashMap()

  /**
   * Registers a new instance to the listeners Map.
   *
   * @param instance that will listen for reports.
   */
  def register (listener: Instance): Unit = {
    listeners += (listener.ip -> listener)
  }

  /**
   * Unregisters an instance from the listeners Map.
   *
   * @param instance that will stop listening for reports.
   */
  def unregister (listener: Instance): Unit = {
    listeners -= listener.ip
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
