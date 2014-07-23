/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import play.api.libs.json._

/**
 * An in simple request that registers
 * the Census Control server information.
 *
 * @param json of the request.
 */
class SetOutReportsRequest (json: JsValue) extends Request {

  val token: String = null

  /** The Census Control hostname. */
  val host: String =
    (json \ "host").asOpt[String] match {
      case None => errors += "'host' field missing."; ""
      case Some(data) => data replaceAll ("http://", "")
    }
 
  /** The Census Control port. */
  var port: Int =
    (json \ "port").asOpt[Int] match {
      case None => errors += "'port' field missing."; 0
      case Some(data) => data
    }


} 
