/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers.requests

import play.api.libs.json._

import http.OutReports

/**
 * An in simple request that registers
 * the Census Control server information.
 *
 * @param json of the request.
 */
class SetOutReportsRequest (json: JsValue) extends Request {

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
  def body: Unit = {
    OutReports.host = host
    OutReports.port = port
  }

} 
