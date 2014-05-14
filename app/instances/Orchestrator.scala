/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import com.github.nscala_time.time.Imports._ 

import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent._
import scala.collection.mutable.Queue

import compute.Sender
import controllers.N4j
import controllers.InReportsListener
import requests.ComputationRequest

object Orchestrator {
  
  def apply (size: Int, requester: ComputationRequest, callback: Orchestrator=>Unit): Orchestrator = {
    val orchestrator = new Orchestrator(size, requester)
    orchestrator.initialize(callback)
    orchestrator
  }

}

/**
 * Module that enqueues requests, processed as first
 * come first served.
 */
class Orchestrator (val size: Int, val algorithm: String, val database: N4j) {

  /** Used to know if the requests are being processed. */
  private var isRunning: Boolean = false

  /** Array that holds all the orchestrated instances. */
  private val pool: Array[Instance] = new Array[Instance](size)

  /** Queue for the requests. */
  private val queue: Queue[Sender] = Queue()

  private def initialize (callback: Orchestrator=>Unit): Unit = {
    for (i <- 0 to (pool.length-1)) {
      pool(i) = Instance({ instance =>
        instance.prepareForAlgorithm(algorithm, database, { () =>
          if (instancesAreReady) callback(this)
        })
      })  
    }
  }

  private def instancesAreReady: Boolean = {
    for (instance <- pool) {
      if (instance.status != InstanceStatus.IDLE) return false
    }
    return true
  }

  /**
   * Adds a request to the queue and starts the
   * processing if the queue is idle.
   *
   * @param req
   */
  def enqueue (request: Sender): Unit = {
    queue += request
    if (!isRunning) {
      isRunning = true
      future { continue }
    }
  }

  /**
   * Executes the next request in the queue, passes 
   * himself as a callback of the request so that 
   * the queue continues after the request is done.
   */
  def continue: Unit = {
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
    // Continue untill instances are full.
    // The queue is restarted when the requests in 
    // the instance are finished.
    if (foundFreeInstance) continue
  }

}
