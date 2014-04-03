/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

import requests._

/** 
 * Module that handles the reports to the Census Control
 * server by sending http requests to it with the report
 * as json.
 */
object HTTPHook extends WebService {

  def isActive: Boolean = (host != "" && port != 0) 

  /**
   * Success reports.
   */
  object Report {
  
//    def computationFinished (request: ComputationRequest): Unit = {
//      println(s"Computation with token:${request.token} finished.")
//    }

  }

  /**
   * Error reports.
   */
  object Error {
    
//    def invalidN4jFormat (request: GraphImportRequest): Unit = {
//      println("Invalid Neo4j format.") 
//    }
//
    def unreachableN4j (request: ComputationRequest): Unit = {
      if (isActive)
        println("Unreachable Neo4j server.")
    }

  }

}
