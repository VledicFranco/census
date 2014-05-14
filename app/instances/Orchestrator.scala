/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import com.github.nscala_time.time.Imports._ 

import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent._
import scala.collection.mutable.Queue

import compute.SingleNodeRequest
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
class Orchestrator (val size: Int, val requester: ComputationRequest) extends InReportsListener {

  /** Used to know if the requests are being processed. */
  private var isRunning: Boolean = false

  /** Array that holds all the orchestrated instances. */
  private val pool: Array[Instance] = new Array[Instance](size)

  /** Queue for the requests. */
  private val queue: Queue[SingleNodeRequest] = Queue()

  private def initialize (callback: Orchestrator=>Unit): Unit = {
    for (i <- 0 to (pool.length-1)) {
      pool(i) = Instance({ instance =>
        instance.prepareForRequest(requester, { () =>
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

  def report (host: String, token: String): Unit = {
    for (instance <- pool) {
      if (instance.host == host) {
        instance.finished(token)
        next
      }
    }
  }

  /**
   * Adds a request to the queue and starts the
   * processing if the queue is idle.
   *
   * @param req
   */
  def enqueue (request: EngineRequest): Unit = {
    queue += request
    if (!isRunning) {
      isRunning = true
      future { next }
    }
  }

  /**
   * Executes the next request in the queue, passes 
   * himself as a callback of the request so that 
   * the queue continues after the request is done.
   */
  private def next: Unit = {
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
    if (foundFreeInstance) next
  }

}
