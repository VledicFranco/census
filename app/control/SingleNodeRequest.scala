/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Trait that sends an EngineRequest for a single node in an
 * all pair graph algorithm, created by a MultiNodeRequest.
 */
trait SingleNodeRequest extends EngineRequest with Sender {

  /** The MultiNodeRequest that created this SingleNodeRequest. */
  val parent: MultiNodeRequest

  /** 
   * Concrete implementation of the complete method of the
   * Sender trait, SingleNodeRequests just report back to their
   * parent MultiNodeRequest when completed.
   */
  def complete: Unit = parent.singleNodeFinished

}
