/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.collection.mutable.Queue

import requests.ControlComputeRequest

/** Interface to create different queue fillers of [[control.EngineRequest]].
  *
  * To fill the requests queue of a [[control.Orchestrator]] with different
  * methods a similar design to a Strategy Pattern is applied:
  *
  *  - --------------------------------------------------------------- -
  *  | trait: QueueFiller                                              |
  *  - --------------------------------------------------------------- -
  *  | concrete # val requestsQueue: Queue[EngineRequest]              |
  *  | abstract # def fillQueue (request: ControlComputeRequest): Unit |
  *  | abstract # def fillingFinished: Unit                            |
  *  - --------------------------------------------------------------- -
  *                            ^                     ^
  *                            |                     |
  *                            |                     |
  *   - ------------------------------------ -       |   - --------------------------------------------------------------- -
  *   | abstract class: Orchestrator         |       |___| trait: AQueueFiller                                             |
  *   - ------------------------------------ -       |   - --------------------------------------------------------------- -
  *   | concrete # def fillingFinished: Unit |       |   | concrete # def fillQueue (request: ControlComputeRequest): Unit |
  *   - ------------------------------------ -       |   - --------------------------------------------------------------- -
  *                                                  | 
  *                                                  |   - --------------------------------------------------------------- -
  *                                                  |___| trait: BQueueFiller                                             |
  *                                                  |   - --------------------------------------------------------------- -
  *                                                  |   | concrete # def fillQueue (request: ControlComputeRequest): Unit |
  *                                                  |   - --------------------------------------------------------------- -
  *                                                  | 
  *                                                  |   - --------------------------------------------------------------- -
  *                                                  |___| trait: CQueueFiller                                             |
  *                                                      - --------------------------------------------------------------- -
  *                                                      | concrete # def fillQueue (request: ControlComputeRequest): Unit |
  *                                                      - --------------------------------------------------------------- -
  *
  *  Each queue filler uses 'fillQueue' to fill the queue on its own way, when
  *  finished it must call 'fillingFinished'. The Orchestrator calls 'fillQueue'
  *  in it's constructor and continues his workflow in 'fillingFinished'.
  *  
  *  When you want to create an Orchestrator with a QueueFiller you just:
  {{{
      val orchestrator1 = new Orchestrator(request) with AQueueFiller
      val orchestrator2 = new Orchestrator(request) with BQueueFiller
      val orchestrator3 = new Orchestrator(request) with CQueueFiller
  }}}
  */
trait QueueFiller {

  /** A queue with all the requests that must be computed by the [[control.Instance]]s. */
  protected val requestsQueue = Queue[EngineRequest]() 

  /** Method to be implemented on each different queue filler. */
  protected def fillQueue (request: ControlComputeRequest): Unit

  /** Method to be implemented by the [[control.Orchestrator]]. */
  protected def fillingFinished: Unit

}
