/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.collection.mutable.Queue

import http.OutReports
import requests.ControlComputeRequest

trait QueueSinglet extends QueueFiller {

  def fillQueue (request: ControlComputeRequest): Unit = {
    requestsQueue.enqueue(new ComputeRequest(request.algorithm, request.vars))
    fillingFinished
  }

}

