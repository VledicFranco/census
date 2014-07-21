/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control 

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import control.requests.ControlComputeRequest
import shared.Utils

/**
 * Main trait that abstracts the attributes of a
 * request to a Census Engine instance.
 */
trait EngineRequest {

  /** A UUID string used to identify the request. */
  val token: String = Utils.genUUID

  /** Json body of the request. */
  val payload: JsValue

}
