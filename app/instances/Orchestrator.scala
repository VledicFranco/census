/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import com.github.nscala_time.time.Imports._ 

import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent._
import scala.collection.mutable.Queue

import compute.EngineRequest

/**
 * Module that enqueues requests, processed as first
 * come first served.
 */
object Orchestrator {

  /** Used to know if the requests are being processed. */
  var isRunning: Boolean = false

  /** Queue for the requests. */
  var queue: Queue[EngineRequest] = Queue()

  var pool: Array[Instance] = new Array[Instance](conf.max_instances) 

  /**
   * Adds a request to the queue and starts the
   * processing if the queue is idle.
   *
   * @param req
   */
  def enqueue (req: EngineRequest): Unit = {
    // Add request to the queue.
    queue += req

    if (isRunning) return
    isRunning = true

    future { next }
  }

  def finished (host: String, token: String): Unit = {
//    val instance = getInstance(host)
//    instance.finished(token)
//    next()
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
    
    var foundFreeInstance = false

    for (instance <- pool) {
      if (instance.hasFreeSpace) {
        foundFreeInstance = true
        instance.enqueue(queue.dequeue)
      }
    }

    // If instances are full and still less than the allowed max
    // create a new instance and start using it.
    
    // Continue untill instances are full.
    // The queue is restarted when the requests in the instances
    // are finished.
    if (foundFreeInstance) next
  }

}
