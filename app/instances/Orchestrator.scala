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

  def createInstances: Unit = {
    val instance = new Instance("localhost", 9000)
    instance.post("/control", "{"
      + """ "host": "localhost", """
      + """ "port": 9001 """
      + "}"
    ) map {
      res => println(res.json)
    } recover {
      case _ => println("error :(")
    }
    pool = Array[Instance] (instance)
  }

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
  def next: Unit = {
    // Terminate if there is no next request.
    if (queue.isEmpty) {
      isRunning = false
      return
    }

    var foundFreeInstance = false

    for (instance: Instance <- pool) {
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
