/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package library

import com.signalcollect._

import requests.EngineComputeRequest
import engine.DirectedGraph
import engine.Resettable
import engine.Database
import http.OutReports

object DirectedSSSP extends DirectedGraph {

  def vertex (id: Any) = new Location(id)

  def edge (target: Any) = new Path(target)

  def computeExecute (request: EngineComputeRequest, vars: Array[String]) = {
    graph.execute
    graph.shutdown
    val query = querystring(vars(0))
    if (query == "MATCH CREATE")
      computeFinish(request, true)
    else
      Database.query(querystring(vars(0)), { (error, response) => 
        if (error)
          computeFinish(request, false)
        else
          computeFinish(request, true)
      })
  }

  private def querystring (source: String): String =
    matchStatement(source) + " " + createStatement(source)

  private def matchStatement (source: String): String =
    vertices.foldLeft("MATCH ") { (st, v) =>
      v._2.state match {
        case None => st
        case Some(state) =>
          if (state == 0) st
          else st+"(a"+source+" {id:'"+source+"'}),(b"+v._1+" {id:'"+v._1+"'}),"
      }
    }.dropRight(1)

  private def createStatement (source: String): String = 
    vertices.foldLeft("CREATE ") { (st, v) => 
      v._2.state match {
        case None => st
        case Some(state) => 
          if (state == 0) st
          else st+"(a"+source+")-[:SHORTEST_PATH {length:"+state+"}]->(b"+v._1+"),"
      }
    }.dropRight(1)

}

class Location(id: Any, initialState: Option[Int] = None) extends DataFlowVertex(id, initialState) with Resettable {

  type Signal = Int
  def collect(signal: Int) = state match {
    case None                      => Some(signal)
    case Some(currentShortestPath) => Some(math.min(currentShortestPath, signal))
  }

  def reset (vars: Array[String]) = {
    if (vars(0) == id) state = Some(0)
    else state = None
    lastSignalState = None
  }

  override def scoreSignal: Double = { 
    if (state.isDefined && (!lastSignalState.isDefined || !lastSignalState.get.isDefined || state.get != lastSignalState.get.get)) 1.0 
    else 0.0 
  }

}

class Path(t: Any) extends OptionalSignalEdge(t) with Resettable {

  def signal = source.state match {
    case None                => None
    case Some(distance: Int) => Some(distance + weight.toInt)
  }

  def reset (vars: Array[String]) = {}

}

