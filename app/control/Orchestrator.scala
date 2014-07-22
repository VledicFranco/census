/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.concurrent._
import scala.collection.mutable.Map
import scala.collection.mutable.Queue

import play.api.libs.json._

import requests.ControlComputeRequest
import shared.Neo4j
import shared.Utils
import shared.Log

/**
 * Class that creates and orchestrates a certain amount of Census Engine instances.
 *
 * @param size or amount of instances to be created and orchestrated.
 * @param algorithm to format the Census Engine services.
 * @param database to be used to import the graph to the Census Engine services.
 */
abstract class Orchestrator (request: ControlComputeRequest) extends QueueFiller {

  private val importRequest = new ImportRequest(request.algorithm, request.dbTag, request.database)

  protected def fillingFinished = createAndInitInstances

  private def createAndInitInstances = {
    for (n <- 1 to request.numberOfInstances) {
      Instance(request.local, instanceReport, instanceError, { instance =>
        instance.import(importRequest)
      })
    }
  }

  private def instanceReport (instance: Instance, token: String) =
    synchronized {
      if (!requestsQueue.isEmpty)
        instance.compute(requestsQueue.dequeue)
      else
        instance.delete { () => Log.info(s"deleted: ${instance.host}") }
    }

  private def instanceError (instance: Instance, token: String, error: String) = {
    Log.error(s"${instance.host} :: $error")
    if (token == importRequest.token) {
      instance.import(importRequest)
    }
  }

}
