/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.collection.mutable.Queue

import http.OutReports
import requests.ControlComputeRequest

/** Adds only one request to the queue. */
trait QueueSinglet extends QueueFiller {

  /** Adds only one request to the queue with the required algorithm
    * and vars from the [[requests.ControlComputeRequest]]
    * 
    * @param request with all the needed data for the filling.
    */
  def fillQueue (request: ControlComputeRequest): Unit = {
    requestsQueue.enqueue(new ComputeRequest(request.algorithm, request.vars))
    fillingFinished
  }

}

