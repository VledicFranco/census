/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.collection.mutable.Queue

import http.OutReports
import requests.ControlComputeRequest

trait QueueSinglet extends QueueFiller {

  protected val requestsQueue = Queue[EngineRequest]() 

  def fillQueue (request: ControlComputeRequest): Unit = {
    requestsQueue.enqueue(new ComputeRequest(request.algorithm, request.vars))
    fillingFinished
  }

}

