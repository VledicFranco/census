/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import com.github.nscala_time.time.Imports._ 

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import http.OutReports

/**
 * Trait used for all pair graph algorithms, called by a 
 * ControlComputeRequest and then creates multiple SingleNodeRequests 
 * that will be sent to Census Engine instances.
 */
trait MultiNodeRequest extends EngineRequest with Receiver {

  /** Orchestrator to create the instances to send the SingleNodeRequests. */
  protected var orchestrator: Orchestrator = null

  /** Number of SingleNodeRequests. */
  protected var numNodes: Int = 0

  /** Number of completed SingleNodeRequests. */
  private var numCompletedNodes: Int = 0

  /**
   * Invoked when all the SingleNodeRequests are done,
   * deletes the orchestrator and reports back to the
   * HTTP hook if existent.
   */
  private def complete: Unit = {
    completed = true
    requester.computationTime = System.currentTimeMillis - creationTime
    OutReports.Report.computationFinished(requester)
    orchestrator.delete { () =>
      println(s"${DateTime.now} - INFO: All instances deleted.")
    }
  }

  /**
   * Invoked when one of the SingleNodeRequest finishes,
   * keeps the sum of completed SingleNodeRequests and 
   * calls the complete method for this request when all
   * the SingleNodeRequests are done.
   */
  def singleNodeFinished: Unit = {
    numCompletedNodes += 1
    if (numNodes == numCompletedNodes) complete
    else orchestrator.continue
  }

}
