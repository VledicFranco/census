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
  * @constructor creates an orchestrator that manages a queue built from a census control compute request.
  * @param request with all the orchestration data.
  */
abstract class Orchestrator (request: ControlComputeRequest) extends QueueFiller {

  fillQueue(request)

  private var finished: Boolean = false

  private val importRequest: ImportRequest = new ImportRequest(request.algorithm, request.dbTag, request.database)

  protected def fillingFinished: Unit =
    if (requestsQueue.isEmpty)
      OutReports.Error.emptyNeo4j(request)
    else
      createAndInitInstances

  private def createAndInitInstances: Unit =
    if (request.engines.length > 0)
      for (server <- request.engines)
        Instance(server._1, server._1, server._2, instanceReport, instanceError, { instance =>
          instance.importGraph(importRequest)
        })
    else
      for (n <- 1 to request.numberOfInstances)
        Instance(instanceReport, instanceError, { instance =>
          instance.importGraph(importRequest)
        })

  private def instanceReport (instance: Instance, token: String): Unit = synchronized {
    if (!requestsQueue.isEmpty)
      instance.compute(requestsQueue.dequeue)
    else {
      instance.delete { Unit => Log.info(s"SHUTDOWN: ${instance.host}") }
      finishAndReportBack
    }
  }

  private def instanceError (instance: Instance, token: String, error: String): Unit = {
    Log.error(s"${instance.host} $error")
    if (token == importRequest.token) {
      instance.importGraph(importRequest)
    }
  }

  private def finishAndReportBack: Unit =
    if (!finished) {
      finished = true
      OutReports.Report.controlComputeFinished(request)
    }

}
