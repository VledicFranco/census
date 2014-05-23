/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import com.github.nscala_time.time.Imports._

import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

import requests._

/** 
 * Module that handles the reports to an external web service
 * by sending http requests to it with the report as json.
 */
object HTTPHook extends WebService {

  def report (token: String): Unit = {
    if (host == "unset") return
    post("/censuscontrol/report", "{"
      +s""" "token": "$token", """
      + """ "status": "success" """
      + "}"
    ) recover {
      case _ => println(s"${DateTime.now} - WARNING: Unreachable HTTP hook server.")
    }
  }

  def error (token: String, error: String): Unit = {
    if (host == "unset") return
    post("/censuscontrol/error", "{"
      +s""" "token": "$token", """
      + """ "status": "error", """
      +s""" "error": "$error", """
      + "}"
    ) recover {
      case _ => println(s"${DateTime.now} - WARNING: Unreachable HTTP hook server.")
    }
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
    
    def invalidN4jFormat (request: ComputationRequest): Unit = {
      error(request.token, "invalid-neo4j-format")
      println(s"${DateTime.now} - ERROR: Invalid Neo4j format.") 
    }

    def unreachableN4j (request: ComputationRequest): Unit = {
      error(request.token, "unreachable-neo4j")
      println(s"${DateTime.now} - ERROR: Unreachable Neo4j server.")
    }

  }

}
