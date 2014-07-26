/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.collection.mutable.Queue

import requests.ControlComputeRequest

trait QueueFiller {

  protected val requestsQueue = Queue[EngineRequest]() 

  protected def fillQueue (request: ControlComputeRequest): Unit

  protected def fillingFinished: Unit

}
