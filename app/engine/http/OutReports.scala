/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import scala.concurrent.Future

import com.github.nscala_time.time.Imports._

import play.api.libs.json._

import controllers.requests.{ImportRequest, ComputeRequest}

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
    post("/censusengine/report", data, { (response, error) => 
      if (error) println(s"${DateTime.now} - WARNING: Unreachable Census Control server.")
    })
  }

  def error (token: String, error: String, on: String): Unit = {
    if (host == "unset") return
    val data = Json.obj(
      "token" -> token,
      "status" -> "error",
      "error" -> error,
      "on" -> on
    )
    post("/censusengine/error", data, { (response, error) => 
      if (error) println(s"${DateTime.now} - WARNING: Unreachable Census Control server.")
    })
  }

  /**
   * Success reports.
   */
  object Report {
  
    def importFinished (request: ImportRequest): Unit = {
      report(request.token)
      println(s"${DateTime.now} - REPORT: Graph import finished in: ${request.importTime} ms.")
    }

    def computationFinished (request: ComputeRequest): Unit = {
      report(request.token)
      println(s"${DateTime.now} - REPORT: Computation with token:${request.token} finished in: ${request.computationTime} ms.")
    }

  }

  /**
   * Error reports.
   */
  object Error {
    
    /** Graph import error reports: */

    def unreachableNeo4j (request: ImportRequest): Unit = {
      error(request.token, "unreachable-neo4j", "graph-import")
      println(s"${DateTime.now} - ERROR: Unreachable Neo4j server on graph import.")
    }

    def importFailed (request: ImportRequest): Unit = {
      error(request.token, "import-failed", "graph-import")
      println(s"${DateTime.now} - ERROR: Graph import failed.") 
    }

    /** Computation error reports: */

    def unreachableNeo4j (request: ComputeRequest): Unit = {
      error(request.token, "unreachable-neo4j", "compute")
      println(s"${DateTime.now} - ERROR: Unreachable Neo4j server on compute.")
    }

    def computationFailed (request: ComputeRequest): Unit = {
      error(request.token, "computation-failed", "compute")
      println(s"${DateTime.now} - ERROR: Computation failed.")
    }

    def computationNotReady (request: ComputeRequest): Unit = {
      error(request.token, "missing-graph", "compute")
      println(s"${DateTime.now} - ERROR: Couldn't start computation, the graph was not properly imported.")
    }

  }

}
