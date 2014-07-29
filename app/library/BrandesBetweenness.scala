/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package library

import scala.collection.mutable.Map
import scala.collection.mutable.Stack
import scala.collection.mutable.MutableList

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import com.signalcollect._
import com.signalcollect.configuration.ExecutionMode

import requests.EngineComputeRequest
import engine.UndirectedGraph
import engine.Resettable
import engine.Database
import http.OutReports

object BrandesStack extends Stack[BrandesVertex]

/**
 * Module that computes the Freeman's centrality 'Closeness' for
 * a single source vertex. It uses the Single Source Shortest Path 
 * algorithm for the sum of shortest paths.
 */
object BrandesBetweenness extends UndirectedGraph  {

  def vertex (id: Any) = new BrandesVertex(id)

  def edge (target: Any) = new BrandesEdge(target)

  def computeExecute (request: EngineComputeRequest, variables: Array[String]): Unit = {
    graph.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.Synchronous))
    graph.shutdown
    // Brandes calculation.
    val reportNodes: MutableList[BrandesVertex] = MutableList()
    val n = vertices.size
    while (!BrandesStack.isEmpty) {
      val w = BrandesStack.pop
      for (v <- w.predecessors) v.delta = v.delta + (v.sigma.toDouble / w.sigma) * (1 + w.delta)
      if (w.id != variables(0) && w.delta > 0) {
        // Normalization.
        //w.delta = w.delta / ((n-2)*(n-1))
        reportNodes += w
      }
    }
    report(request, reportNodes.toList) 
    reportNodes.clear
  }

  private def report (request: EngineComputeRequest, ns: List[BrandesVertex]): Unit =
    Database.query(matchStatement(ns) + " " + setStatement(ns), { (error, response) => 
      if (error)
        computeFinish(request, false)
      else
        computeFinish(request, true)
    })

  private def matchStatement (ns: List[BrandesVertex]): String = 
    addMatchStatement(ns, "MATCH ")

  private def addMatchStatement (ns: List[BrandesVertex], st: String): String =
    if (ns.isEmpty) st.dropRight(1)
    else addMatchStatement(ns.tail, st+"(a"+ns.head.id+" {id:'"+ns.head.id+"'}),")

  private def setStatement (ns: List[BrandesVertex]): String =
    addSetStatement(ns, "SET ")

  private def addSetStatement (ns: List[BrandesVertex], st: String): String =
    if (ns.isEmpty) st.dropRight(1)
    else addSetStatement(ns.tail, st+"a"+ns.head.id+".betweenness = a"+ns.head.id+".betweenness + "+ns.head.delta+",")

}

class BrandesEdge(t: Any) extends OptionalSignalEdge(t) with Resettable {
  def reset (varibles: Array[String]) = {}
  def signal = {
    source.state match {
      case None => None
      case Some(distance: Int) => Some((distance + weight.toInt, source))
    }
  }
}

class BrandesVertex(vertexId: Any, initialState: Option[Int] = None) extends DataFlowVertex(vertexId, initialState) with Resettable {
  type Signal = Tuple2[Int, BrandesVertex]

  var sigma: Int = 0
  var delta: Double = 0.0
  val predecessors = MutableList[BrandesVertex]()

  def reset (variables: Array[String]): Unit = {
    if (vertexId == variables(0)) {
      state = Some(0)
      sigma = 1
      BrandesStack.push(this)
    }
    else {
      state = None
      sigma = 0
    }
    delta = 0.0
    predecessors.clear
    lastSignalState = None
  }

  def collect(signal: Tuple2[Int, BrandesVertex]) = {
    val source = signal._2
    val signalState = signal._1
    val sourceState = source.state.get
    val newState = state match {
      case None => 
        BrandesStack.push(this)
        Some(signalState)
      case Some(currentShortestPath) => 
        Some(math.min(currentShortestPath, signalState))
    }
    if (sourceState + 1 == newState.get) {
      sigma = sigma + source.sigma
      predecessors += source
    }
    newState
  }

  override def scoreSignal: Double = {
    if (state.isDefined && (!lastSignalState.isDefined || !lastSignalState.get.isDefined || state.get != lastSignalState.get.get)) {
      1.0
    } else {
      0.0
    }
  }

}
