/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute.library

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import compute.MultiNodeRequest
import shared.Neo4j
import controllers.HTTPHook
import control.requests.ControlComputeRequest
import instances.Orchestrator
import instances.Instance

/**
 * MultiNodeRequest implementation for Freeman's Closeness.
 */
class BrandesBetweennessMN (val requester: ControlComputeRequest) extends MultiNodeRequest {
  
  /**
   * Called by the ControlComputeRequest to initiate this
   * EngineRequest. Creates an orchestrator formated for the computation of
   * Single Source Closeness algorithms.
   */
  def receive: Unit = {
    orchestrator = Orchestrator(requester.numberOfInstances, "BrandesBetweenness", requester.database, { orchestrator =>
      batch
    })
  }

  /**
   * Invoked for every node in the graph database with the provided tag.
   * Creates a single node request for every node
   * and enqueues it to the orchestrator.
   */
  private def enqueueToOrchestrator (nodeId: String): Unit = {
    numNodes += 1
    orchestrator.enqueue(new BrandesBetweennessSN(nodeId, this, requester))
  }

  /** 
   * Queries Neo4j for a batch of 1000 vertices ids.
   */
  private def batch: Unit = {
    requester.database.query(
       s"MATCH (n:${requester.database.tag}) "
      + "WHERE not(has(n.censuscheckbrandesbetweenness)) "
      + "WITH n LIMIT 1000 "
      +s"MATCH (n)--(r:${requester.database.tag}) "
      + "SET n.censuscheckbrandesbetweenness = true "
      + "RETURN n.id"
    ) map {
      res => validateJson(res.json)
    } recover {
      // Report: Neo4j server unreachable.
      case _ => HTTPHook.Error.unreachableNeo4j(requester)
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
        else requester.database.query(s"MATCH (n:${requester.database.tag} {censuscheckbrandesbetweenness:true}) REMOVE n.censuscheckbrandesbetweenness") recover {
          // Report: Neo4j server unreachable.
          case _ => HTTPHook.Error.unreachableNeo4j(requester)
        }
        // FINISHED
      case None => 
        // Report: Invalid Neo4j graph format.
        HTTPHook.Error.invalidNeo4jFormat(requester)
    }
  }

  /**
   * Imports an already parsed json response from Neo4j.
   * If it receives an empty array, it assumes that there 
   * are no more nodes to import.
   *
   * @param data the parsed array with the triplets from Neo4j.
   * @return 'true' if there is still more batch imports to do.
   *         'false' if there are no more batch imports to do.
   */
  private def importArray (data: Array[Array[String]]): Boolean = {
    if (!data.isEmpty) {
      for (source: Array[String] <- data) {
        enqueueToOrchestrator(source(0))
      }
      return true
    } else {
      return false
    }
  }

}
