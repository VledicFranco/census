/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package engine

import scala.collection.mutable.Map
import scala.collection.mutable.MutableList

import com.signalcollect.{Graph => SignalCollectGraph}
import com.signalcollect.GraphBuilder
import com.signalcollect.Vertex
import com.signalcollect.Edge

import http.OutReports
import requests.EngineImportRequest
import requests.EngineComputeRequest

/**
 * This trait has the interfaces and functionality needed for
 * a graph import process.
 */
trait Graph {

  /** Changed to 'true' only if the graph was succesfully imported. */
  var computationReady: Boolean = false

  /** The signal collect graph. */
  var graph: SignalCollectGraph[Any, Any] = null

  /** The imported vertices. */
  val vertices: Map[Any, Vertex[Any, _]] = Map()

  /** The imported edges map. (Source ID -> Edges) */
  val edges: Map[Any, MutableList[Edge[Any]]] = Map()

  /**
   * Used to execute the graph import for the algorithm.
   *
   * @param importRequest for this import.
   */
  def importExecute (importRequest: EngineImportRequest): Unit 

  /**
   * Used by a EngineImportRequest to start the Database graph
   * importation.
   *
   * @param importRequest for this import.
   */
  def importStart (importRequest: EngineImportRequest): Unit = importExecute(importRequest)

  /**
   * Used when the import finishes. Reports back to Census Control
   * and sets this algorithm as ready or not ready.
   *
   * @param importRequest for this import.
   * @param success 'true' if the importation was successful.
   */
  def importFinish (importRequest: EngineImportRequest, successful: Boolean): Unit = {
    if (successful) {
      OutReports.Report.engineImportFinished(importRequest)
      computationReady = true
    } else {
      OutReports.Error.importFailed(importRequest)
      computationReady = false
    }
  }

  /** 
   * Runs the algorithm.
   *
   * @param computationRequest for this execution.
   * @param variables received for this execution.
   */
  def computeExecute (computationRequest: EngineComputeRequest, variables: Array[String]): Unit

  /** 
   * Used by a EngineComputeRequest to start the algorithm.
   *
   * @param computationRequest for this execution.
   * @param variables received for this execution.
   */
  def computeStart (computationRequest: EngineComputeRequest, variables: Array[String]): Unit = {
    if (!computationReady)
      OutReports.Error.computationNotReady(computationRequest)
    else {
      reset(variables)
      computeExecute(computationRequest, variables)
    }
  }

  /** 
   * Invoked when the computation finishes, registers computation time,
   * and reports success or failure, then lets the next computation in the
   * queue begin.
   *
   * @param computationRequest for this execution.
   * @param success 'true' if the computation was successful.
   */
  def computeFinish (computationRequest: EngineComputeRequest, success: Boolean): Unit = {
    if (success) 
      OutReports.Report.engineComputeFinished(computationRequest)
    else 
      OutReports.Error.computationFailed(computationRequest)
  }

  /**
   * Used to clear the imported graph. (Important for memory management)
   */
  def clear: Unit = {
    vertices.clear
    edges.clear
    computationReady = false
  }

  /**
   * Used to setup the signalcollect's graph by adding all the 
   * imported vertices and edges before every computation.
   */
  def reset (variables: Array[String]): Unit = {
    graph = GraphBuilder.build
    vertices.foreach { case (key, vertex) =>
      vertex.asInstanceOf[Resettable].reset(variables)
      graph.addVertex(vertex)
    }
    edges.foreach { case (key, array) =>
      for (edge <- array) {
        edge.asInstanceOf[Resettable].reset(variables)
        graph.addEdge(key, edge)
      }
    }
  }

}
