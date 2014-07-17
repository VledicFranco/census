/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers.requests

import play.api.libs.json._

import controllers.OutReports

/**
 * Companion object to correctly build the request.
 */
object SetOutReportsRequest {

  /**
   * Constructor that creates the request, and executes
   * the validation.
   *
   * @param json of the request.
   * @return a request instance.
   */
  def apply (json: JsValue): SetOutReportsRequest = {
    val req = new SetOutReportsRequest(json)
    req.validate
    req.execute
    req
  }

}

/**
 * An in simple request that registers
 * the Census Control server information.
 *
 * @param json of the request.
 */
class SetOutReportsRequest (val json: JsValue) extends Request {

  /** The Census Control hostname. */
  var host: String = null

  /** The Census Control port. */
  var port: Int = 0

  /**
   * Json validation.
   */
  def validate: Unit = {
    (json \ "host").asOpt[String] match {
      case None => errors = errors :+ "'host' field missing."
      case Some(data) => host = data replaceAll ("http://", "")
    }
    (json \ "port").asOpt[Int] match {
      case None => errors = errors :+ "'port' field missing."
      case Some(data) => port = data
    }
  }

  /**
   * Request execution.
   */
  def execute: Unit = {
    OutReports.host = host
    OutReports.port = port
  }

} 
