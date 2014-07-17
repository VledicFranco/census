/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map

import com.signalcollect._

import controllers.OutReports
import controllers.requests.EngineImportRequest

/**
 * This trait has the interfaces and functionality needed for
 * a graph import process.
 */
trait GraphImport extends GraphCompute {
  
  /** The signal collect graph. */
  var graph: Graph[Any, Any] = null

  /** The imported vertices. */
  val vertices: Map[Any, Vertex[Any, _]] = Map()
  
  /** The imported edges map. (Source ID -> Edges) */
  val edges: Map[Any, ArrayBuffer[Edge[Any]]] = Map()

  /**
   * Used to execute the graph import for the algorithm.
   *
   * @param importRequest for this import.
   */
  def importExecute (importRequest: EngineImportRequest): Unit 

  /**
   * Used by a EngineImportRequest to start the Neo4j graph
   * importation.
   *
   * @param importRequest for this import.
   */
  def importStart (importRequest: EngineImportRequest): Unit = {
    importRequest.importTime = System.currentTimeMillis
    importExecute(importRequest)
  }

  /**
   * Used when the import finishes. Reports back to Census Control
   * and sets this algorithm as ready or not ready.
   *
   * @param importRequest for this import.
   * @param success 'true' if the importation was successful.
   */
  def importFinish (importRequest: EngineImportRequest, successful: Boolean): Unit = {
    importRequest.importTime = System.currentTimeMillis - importRequest.importTime
    if (successful) {
      OutReports.Report.importFinished(importRequest)
      computationReady = true
    } else {
      OutReports.Error.importFailed(importRequest)
      computationReady = false
    }
    importRequest.finish()
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
