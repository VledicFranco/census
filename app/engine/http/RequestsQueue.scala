/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import scala.concurrent._
import scala.collection.mutable.Queue

import play.api.libs.concurrent.Execution.Implicits._

import requests.QueueRequest

/**
 * Module that enqueues requests, processed as first
 * come first served.
 */
object RequestsQueue {

  /** Used to know if the requests are being processed. */
  var isRunning = false

  /** Queue for the requests. */
  var queue = Queue[QueueRequest]()

  /**
   * Adds a request to the queue and starts the
   * processing if the queue is idle.
   *
   * @param req
   */
  def enqueue (req: QueueRequest): Unit = {
    // Add request to the queue.
    queue += req
    if (isRunning) return
    else isRunning = true
    // Async queue start.
    future {
      next()
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
    // Retrieve next request and execution 
    // procedure of the request.
    queue.dequeue.init(next)
  }

}
