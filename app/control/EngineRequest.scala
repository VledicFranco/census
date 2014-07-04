/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import controllers.requests.ComputationRequest
import utils.Utils

/**
 * Main trait that abstracts the attributes of a
 * request to a Census Engine instance.
 */
trait EngineRequest {

  /** A UUID string used to identify the request. */
  val token: String = Utils.genUUID

  /** 
   * The request that arrived to Census Control,
   * responsible of creating this Census Engine
   * request.
   */
  val requester: ComputationRequest

  /** State flag that marks if this request was completed. */
  var completed: Boolean = false

  /** The moment when this request was created. */
  val creationTime: Long = System.currentTimeMillis

}
