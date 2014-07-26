/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute.library

import com.signalcollect._

import requests.EngineComputeRequest
import engine.UndirectedGraph
import engine.Resettable
import engine.Database
import http.OutReports

object SSCloseness extends UndirectedGraph {

  def vertex (id: Any) = new Location(id)

  def edge (target: Any) = new Path(target)

  def computeExecute (computeRequest: EngineComputeRequest, variables: Array[String]): Unit = {
    graph.execute
    graph.shutdown
    var n = 0
    var sum = 0
    val source = variables(0)
    vertices.foreach { case (id, vertex) =>
      val v = vertex.asInstanceOf[Location]
      if (v.state.isDefined && id != source) {
        n = n + 1
        sum = sum + v.state.get
      }
    }
    val closeness = sum/(n).toDouble
    Database.query(s"MATCH (n {id:'$source'}) SET n.closeness=$closeness", { (error, response) =>
      if (error) return computeFinish(computeRequest, false)
      computeFinish(computeRequest, true)
    })
  }

  class Path(t: Any) extends OptionalSignalEdge(t) with Resettable {

    def signal = {
      source.state match {
        case None => None
        case Some(distance: Int) => Some(distance + weight.toInt)
      }
    }

    def reset (variables: Array[String]) = {}
  }
  
  class Location(vertexId: Any, initialState: Option[Int] = None) extends DataFlowVertex(vertexId, initialState) with Resettable {
    type Signal = Int

    def collect(signal: Int) = {
      state match {
        case None => Some(signal)
        case Some(currentShortestPath) => Some(math.min(currentShortestPath, signal))
      }
    }

    def reset (variables: Array[String]) = {
      if (vertexId == variables(0)) state = Some(0)
      else state = None
      lastSignalState = None
    }

    override def scoreSignal: Double = {
      if (state.isDefined && (!lastSignalState.isDefined || !lastSignalState.get.isDefined || state.get != lastSignalState.get.get)) 1.0
      else 0.0
    }
  }

}
