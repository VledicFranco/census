/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import scala.concurrent.Future

import com.github.nscala_time.time.Imports._

import play.api.libs.json._

import requests.Request
import requests.EngineImportRequest
import requests.EngineComputeRequest
import requests.ControlComputeRequest

/** 
 * Module that handles the reports to the Census Control
 * server by sending http requests to it with the report
 * as json.
 */
object OutReports extends WebService {

  def report (token: String): Unit = {
    if (host == "unset") return
    val data = Json.obj(
      "token" -> token,
      "status" -> "success"
    )
    post("/census/report", data, { (response, error) => 
      if (error) println(s"${DateTime.now} - WARNING: Unreachable Census Control server.")
    })
  }

  def error (token: String, error: String): Unit = {
    if (host == "unset") return
    val data = Json.obj(
      "token" -> token,
      "status" -> "error",
      "error" -> error
    )
    post("/census/error", data, { (response, error) => 
      if (error) println(s"${DateTime.now} - WARNING: Unreachable Census Control server.")
    })
  }

  /**
   * Success reports.
   */
  object Report {
  
    def engineImportFinished (request: EngineImportRequest): Unit = {
      report(request.token)
      println(s"${DateTime.now} - REPORT: Graph import finished in: ${request.importTime} ms.")
    }

    def engineComputeFinished (request: EngineComputeRequest): Unit = {
      report(request.token)
      println(s"${DateTime.now} - REPORT: Computation with token:${request.token} finished in: ${request.computationTime} ms.")
    }

    def controlComputeFinished (request: ControlComputeRequest): Unit = {
      report(request.token)
      println(s"${DateTime.now} - REPORT: Computation with token:${request.token} finished in: ${request.computationTime} ms.")
    }

  }

  /**
   * Error reports.
   */
  object Error {
    
    /** Graph import error reports: */

    def unreachableNeo4j (request: Request): Unit = {
      error(request.token, "unreachable-neo4j")
      println(s"${DateTime.now} - ERROR: Unreachable Neo4j server on graph import.")
    }

    def importFailed (request: Request): Unit = {
      error(request.token, "import-failed")
      println(s"${DateTime.now} - ERROR: Graph import failed.") 
    }

    /** Computation error reports: */

    def computationFailed (request: Request): Unit = {
      error(request.token, "computation-failed")
      println(s"${DateTime.now} - ERROR: Computation failed.")
    }

    def computationNotReady (request: Request): Unit = {
      error(request.token, "missing-graph")
      println(s"${DateTime.now} - ERROR: Couldn't start computation, the graph was not properly imported.")
    }

  }

}
