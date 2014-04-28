/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import scala.concurrent._
import scala.collection.mutable.Queue

import play.api.libs.concurrent.Execution.Implicits._

import compute.EngineAlgorithm

/**
 * Module that enqueues requests, processed as first
 * come first served.
 */
object Orchestrator {

  /** Used to know if the requests are being processed. */
  var isRunning = false

  /** Queue for the requests. */
  var queue: Queue[EngineAlgorithm] = Queue()

  var pool: Array[Instance] = null

  /**
   * Adds a request to the queue and starts the
   * processing if the queue is idle.
   *
   * @param req
   */
  def enqueue (req: EngineAlgorithm): Unit = {
    // Add request to the queue.
    queue.synchronized {
      queue += req
    }
    if (isRunning) return
    isRunning = true
    // Async queue start.
    future {
      next
    }
  }

  /**
   * Executes the next request in the queue, passes 
   * himself as a callback of the request so that 
   * the queue continues after the request is done.
   */
  private def next (): Unit = {
    // Terminate if there is no next request.
    if (queue.isEmpty) {
      isRunning = false
      return
    }
    // Retrieve next request.
    val req: EngineAlgorithm = queue.dequeue
    // Call execution procedure of the request.

    // Check for the InstancesPool status.
    // Enqueue request to the apropiate instance.
  }

}
