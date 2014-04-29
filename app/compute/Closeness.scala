/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import requests.Utils
import requests.ComputationRequest
import controllers.N4j
import controllers.HTTPHook
import instances.Orchestrator
import instances.Instance

object Closeness {

  def apply (r: ComputationRequest): Closeness = {
    val algo = new Closeness(r)
    algo
  }

}

class Closeness (val r: ComputationRequest) extends EngineAlgorithm {

  requester = r
  token = requester.token
  
  def enqueue: Unit = {
    batch
  }

  /** 
   * Queries Neo4j for a batch of 1000 vertices ids.
   */
  private def batch: Unit = {
    database.query(
      s"MATCH (n:${database.tag}) "
      + "WHERE not(has(n.censuscheck)) "
      + "WITH n LIMIT 1000 "
      + "SET n.censuscheck = true "
      + "RETURN n.id"
    ) map {
      res => validateJson(res.json)
    } recover {
      // Report: Neo4j server unreachable.
      case _ => HTTPHook.Error.unreachableN4j(r)
    }
  }

  /**
   * Validates the response from a batch, reports any format error
   * and starts the next batch or cleans the Neo4j database from
   * the import to finish the request.
   *
   * @param json to be validated.
   */
  private def validateJson (json: JsValue): Unit = {
    (json \ "data").asOpt[Array[Array[String]]] match {
      case Some(data) => 
        // Keep importing arrays until an empty one is received.
        if (importArray(data)) batch
        // Clear database from censusimport attributes.
        else database.query(s"MATCH (n:${database.tag} {censuscheck:true}) REMOVE n.censuscheck") recover {
          // Report: Neo4j server unreachable.
          case _ => HTTPHook.Error.unreachableN4j(r)
        }
        // FINISHED
      case None => 
        // Report: Invalid Neo4j graph format.
        HTTPHook.Error.invalidN4jFormat(r)
    }
  }

  /**
   * Imports an already parsed json response from Neo4j, this method
   * uses the abstract method 'insertTriple'. If it receives an empty
   * array, it assumes that there are no more nodes to import.
   *
   * @param data the parsed array with the triplets from Neo4j.
   * @return 'true' if there is still more batch imports to do.
   *         'false' if there are no more batch imports to do.
   */
  private def importArray (data: Array[Array[String]]): Boolean = {
    if (!data.isEmpty) {
      for (source: Array[String] <- data) {
        // Create SSCloseness request and enqueue.
        val sscloseness = SSCloseness(source(0), r)
        sscloseness.token = Utils.genUUID
        sscloseness.parentToken = token
        sscloseness.database = database
        sscloseness.enqueue
      }
      return true
    } else {
      return false
    }
  }
  
  def sendRequest (instance: Instance): Unit = {}

  def computationComplete: Unit = {}

}
