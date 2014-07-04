/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import com.github.nscala_time.time.Imports._

import play.api.libs.json._

import controllers.requests.ComputationRequest

/** 
 * Module that handles the reports to an external web service
 * by sending http requests to it with the report as json.
 */
object HTTPHook extends WebService {

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
  
    def computationFinished (request: ComputationRequest): Unit = {
      report(request.token)
      println(s"${DateTime.now} - REPORT: Computation with token:${request.token} finished in: ${request.computationTime} ms.")
    }

  }

  /**
   * Error reports.
   */
  object Error {
    
    def invalidNeo4jFormat (request: ComputationRequest): Unit = {
      error(request.token, "invalid-neo4j-format")
      println(s"${DateTime.now} - ERROR: Invalid Neo4j format.") 
    }

    def unreachableNeo4j (request: ComputationRequest): Unit = {
      error(request.token, "unreachable-neo4j")
      println(s"${DateTime.now} - ERROR: Unreachable Neo4j server.")
    }

  }

}
