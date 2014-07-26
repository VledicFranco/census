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

/** 
 * Module that handles the reports to the Census Control
 * server by sending http requests to it with the report
 * as json.
 */
object OutReports {

  var service: WebService = null

  def setService (_host: String, _port: Int) = service = new WebService {
    val host = _host
    val port = _port
    val user = null
    val password = null
  }

  def report (token: String): Unit = {
    if (service == null) return
    val data = Json.obj("token" -> token)
    service.post("/census/report", data, { (error, response) => 
      if (error) Log.error("Unreachable Census Control server.")
    })
  }

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

  /**
   * Success reports.
   */
  object Report {

    def engineImportFinished (request: Request): Unit = {
      report(request.token)
      Log.info(s"Graph import finished.")
    }

    def engineComputeFinished (request: Request): Unit = {
      report(request.token)
      Log.info(s"Computation with token:${request.token} finished.")
    }

    def controlComputeFinished (request: Request): Unit = {
      report(request.token)
      Log.info(s"Computation with token:${request.token} finished.")
    }

  }

  /**
   * Error reports.
   */
  object Error {

    /** Graph import error reports: */

    def unreachableNeo4j (request: Request): Unit = {
      error(request.token, "unreachable-neo4j")
      Log.error(s"Unreachable Neo4j server on graph import.")
    }

    def emptyNeo4j (request: Request): Unit = {
      error(request.token, "empty-neo4j")
      Log.error(s"Empty Neo4j database with provided tag.")
    }

    def importFailed (request: Request): Unit = {
      error(request.token, "import-failed")
      Log.error(s"Graph import failed.") 
    }

    /** Computation error reports: */

    def computationFailed (request: Request): Unit = {
      error(request.token, "computation-failed")
      Log.error(s"Computation failed.")
    }

    def computationNotReady (request: Request): Unit = {
      error(request.token, "missing-graph")
      Log.error(s"Couldn't start computation, the graph was not properly imported.")
    }

  }

}
