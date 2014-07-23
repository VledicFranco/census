/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package http

import scala.collection.mutable.Map

import play.api._
import play.api.mvc._
import play.api.libs.json._

import control.Instance

/**
 * Module that receives reports from the Census Engine instances.
 */
object InReports extends Controller {

  /** Map with all the Census Engine instances. */
  private val listeners: Map[String, Instance] = Map()

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

  /** Route: POST /census/report */ 
  def report = Action(parse.json) { implicit request =>
    val token = (request.body \ "token").as[String]
    listeners.apply(request.remoteAddress).report(token)
    Ok
  }

  /** Route: POST /census/error */ 
  def error = Action(parse.json) { implicit request =>
    val token = (request.body \ "token").as[String]
    val error = (request.body \ "error").as[String]
    listeners(request.remoteAddress).error(token, error)
    Ok
  }

}
