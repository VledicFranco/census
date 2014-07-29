/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import play.api.libs.json._

import shared.Log
import http.OutReports
import requests.ControlComputeRequest

/** Orchestrator that manages multiple engine requests between instances.
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
  *
  * @constructor creates an orchestrator that manages a queue built from a census control compute request.
  * @param request with all the orchestration data.
  */
abstract class Orchestrator (request: ControlComputeRequest) extends QueueFiller {

  fillQueue(request)

  /** Changed to 'true' when the queue is empty. */
  private var finished: Boolean = false

  /** A census graph importation request used for every instance. */
  private val importRequest: ImportRequest = new ImportRequest(request.algorithm, request.dbTag, request.database)

  /** Implementation from [[control.QueueFiller]]. Called after the queue is filled.
    * 
    * Sennds an [[http.OutReports.Error.emptyNeo4j]] error if the queue is empty after filling,
    * if not fires the creation of the instances.
    */
  protected def fillingFinished: Unit =
    if (requestsQueue.isEmpty)
      OutReports.Error.emptyNeo4j(request)
    else
      createAndInitInstances

  /** Creates all the needed instances depending on the [[requests.ControlComputeRequest]] parameters.
    * 
    * If a list of census engines servers werer provided it creates instances based on them.
    * If not it creates instnaces based on Google Compute Engine services.
    */
  private def createAndInitInstances: Unit =
    if (request.engines.length > 0)
      for (server <- request.engines)
        Instance(server._1, server._1, server._2, instanceReport, instanceError, { instance =>
          instance.importGraph(importRequest)
        })
    else
      for (n <- 1 to request.numberOfInstances)
        Instance(instanceReport, instanceError, { instance =>
          Log.debug(s"Will start graph importation for ${instance.host}")
          instance.importGraph(importRequest)
        })

  /** Function passed to every created instance, called when an instance receives a report
    * from the actual instance server.
    * 
    * When a Census Engine server finishes a request it uses [[http.OutReports]] to send a http
    * report to this Census Control server through the net, the report arrives at [[http.InReports]]
    * and is passed to the corresponding [[control.Instance]] that is registered in the [[http.InReports]]
    * listeners, then the corresponding [[control.Instance]] calls this function to notify this orchestrator.
    *
    * @param instance that is receiving the report.
    * @param token of the request that is being reported.
    */
  private def instanceReport (instance: Instance, token: String): Unit = synchronized {
    Log.debug(s"Instance ${instance.host} finished request $token")
    if (!requestsQueue.isEmpty)
      instance.compute(requestsQueue.dequeue)
    else {
      instance.delete { Unit => Log.info(s"SHUTDOWN: ${instance.host}") }
      finishAndReportBack
    }
  }

  /** Function passed to every created instance, called when an instance receives an error
    * from the actual instance server.
    *
    * When a Census Engine server fails to finish a request it uses [[http.OutReports]] to send
    * a http error to this Census Control server through the net, the report arrives at [[http.InReports]]
    * and is passed to the corresponding [[control.Instance]] that is registered in the [[http.InReports]]
    * listeners, then the corresponding [[control.Instance]] calls this function to notify this orchestrator.
    * 
    * @param instance that is receiving the error.
    * @param token of the request that failed.
    * @param error type that made the request fail.
    */
  private def instanceError (instance: Instance, token: String, error: String): Unit = {
    Log.error(s"${instance.host} $error")
    if (token == importRequest.token) {
      Log.debug(s"Will retry importation for instance ${instance.host}")
      instance.importGraph(importRequest)
    } else
      instance.compute(requestsQueue.dequeue)
  }

  /** Sends a [[http.OutReports.Report.controlComputeFinished]] only when the 'finished' flag is unset. */
  private def finishAndReportBack: Unit =
    if (!finished) {
      finished = true
      OutReports.Report.controlComputeFinished(request)
    }

}
