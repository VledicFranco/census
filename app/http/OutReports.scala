/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package http

import scala.concurrent.Future

import play.api.libs.json._

import requests.Request
import requests.EngineImportRequest
import requests.EngineComputeRequest
import requests.ControlComputeRequest
import shared.WebService
import shared.Log

/** Module that handles the reports to the Census Control
  * server by sending http requests to it with the report
  * as json.
  */
object OutReports {

  /** [[shared.WebService]] instance to be used. */
  var service: WebService = null

  /** Creates and sets a new [[shared.WebService]] instance.
    *
    * @param _host of the server.
    * @param _port of the server.
    */
  def setService (_host: String, _port: Int): Unit = 
    service = new WebService {
      val host = _host
      val port = _port
      val user = null
      val password = null
    }

  /** Sends a POST to the server reporting a request.
    * 
    * @param token of the request that is being reported.
    */
  def report (token: String): Unit = {
    if (service == null) return
    val data = Json.obj("token" -> token)
    service.post("/census/report", data, { (error, response) => 
      if (error) Log.error("Unreachable Census Control server.")
    })
  }

  /** Sends a POST to the server reporting an error.
    * 
    * @param token of the requests that had the error.
    * @param error that occurred.
    */
  def error (token: String, error: String): Unit = {
    if (service == null) return
    val data = Json.obj(
      "token" -> token,
      "error" -> error
    )
    service.post("/census/error", data, { (error, response) => 
      if (error) Log.error("Unreachable Census Control server.")
    })
  }

  /** Success reports. */
  object Report {

    /** Reports that a graph import finished in a Census Engine instance. */
    def engineImportFinished (request: Request): Unit = {
      report(request.token)
      Log.info(s"Graph import finished.")
    }

    /** Reports that a computation finished in a Census Engine instance. */
    def engineComputeFinished (request: Request): Unit = {
      report(request.token)
      Log.info(s"Computation with token:${request.token} finished.")
    }

    /** Reports that a whole computation finished for a Census Control server. */
    def controlComputeFinished (request: Request): Unit = {
      report(request.token)
      Log.info(s"Computation with token:${request.token} finished.")
    }

  }

  /** Error reports. */
  object Error {

    /** Graph import error reports: */

    /** Reports that the Neo4j database couldn't be reached */
    def unreachableNeo4j (request: Request): Unit = {
      error(request.token, "unreachable-neo4j")
      Log.error(s"Unreachable Neo4j server on graph import.")
    }

    /** Reports that the Neo4j database returned an empty set when importing. */
    def emptyNeo4j (request: Request): Unit = {
      error(request.token, "empty-neo4j")
      Log.error(s"Empty Neo4j database with provided tag.")
    }

    /** Reports that an error occurred when importing the database. */
    def importFailed (request: Request): Unit = {
      error(request.token, "import-failed")
      Log.error(s"Graph import failed.") 
    }

    /** Computation error reports: */

    /** Reports that a computation failed. */
    def computationFailed (request: Request): Unit = {
      error(request.token, "computation-failed")
      Log.error(s"Computation failed.")
    }

    /** Reports that a computation couldn't be done because there was no graph importation before. */
    def computationNotReady (request: Request): Unit = {
      error(request.token, "missing-graph")
      Log.error(s"Couldn't start computation, the graph was not properly imported.")
    }

  }

}
