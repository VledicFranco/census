/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import com.github.nscala_time.time.Imports._ 

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import instances.Orchestrator
import controllers.HTTPHook

trait MultiNodeRequest extends EngineRequest with Receiver {

  protected var orchestrator: Orchestrator = null

  protected var numNodes: Int = 0

  private var numCompletedNodes: Int = 0

  private def complete: Unit = {
    completed = true
    requester.computationTime = System.currentTimeMillis - creationTime
    HTTPHook.Report.computationFinished(requester)
    orchestrator.delete { () =>
      println("INFO - All instances deleted.")
    }
  }

  def singleNodeFinished: Unit = {
    numCompletedNodes += 1
    if (numNodes == numCompletedNodes) complete
    else orchestrator.continue
  }

}
