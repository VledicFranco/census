/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute.library

import com.signalcollect._

import compute.{UndirectedGraphImport, Resettable}
import controllers.{Neo4j, OutReports}
import controllers.requests.ComputeRequest

object SSCloseness extends UndirectedGraphImport {

  def vertex (id: Any) = new Location(id)

  def edge (target: Any) = new Path(target)

  def computeExecute (computationRequest: ComputeRequest, variables: Array[String]): Unit = {
    computationRequest.stats = graph.execute.toString
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
    Neo4j.query(s"MATCH (n {id:'$source'}) SET n.closeness=$closeness", { (response, error) =>
      if (error) return computeFinish(computationRequest, false)
      computeFinish(computationRequest, true)
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
