/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import play.api.libs.json._

import controllers.HTTPHook

/**
 * Companion object to correctly build the request.
 */
object SetHTTPHookRequest {

  /**
   * Constructor that creates the request, and executes
   * the validation.
   *
   * @param json of the request.
   * @return a request instance.
   */
  def apply (json: JsValue): SetHTTPHookRequest = {
    val req = new SetHTTPHookRequest(json)
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
class SetHTTPHookRequest (val json: JsValue) extends SimpleRequest {

  /** The Census Control hostname. */
  var host: String = null

  /** The Census Control port. */
  var port: Int = 0

  /**
   * Json validation.
   */
  override def validate: Unit = {
    (json \ "host").asOpt[String] match {
      case None => errors = errors :+ "'host' field missing."
      case Some(data) => host = data
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
    if (errors.length > 0) return
    HTTPHook.setHost(host, port)
  }

} 
