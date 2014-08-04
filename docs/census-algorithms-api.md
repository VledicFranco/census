Census API for Algorithms
=========================

Census has a very simple API to integrate communication, data and computation
easily on a new graph algorithm implemented with Signal/Collect. To illustrate
this we will implement the Single Source Closeness algorithm.

1 Create the document
---------------------

The algorithm should live in the ´/app/library´ directory, so lets create it

```bash
cd app/library
touch SSCloseness.scala
```

2 Add all the required imports
------------------------------

```scala
package library

import com.signalcollect._

import requests.EngineComputeRequest
import engine.UndirectedGraph
import engine.Resettable
import engine.Database
import http.OutReports
```

3 Add your Signal/Collect classes
---------------------------------

```scala
class Path(t: Any) extends OptionalSignalEdge(t) {

  def signal = {
    source.state match {
      case None => None
      case Some(distance: Int) => Some(distance + weight.toInt)
    }
  }

}

class Location(vertexId: Any, initialState: Option[Int] = None) extends DataFlowVertex(vertexId, initialState) {
  type Signal = Int

  def collect(signal: Int) = {
    state match {
      case None => Some(signal)
      case Some(currentShortestPath) => Some(math.min(currentShortestPath, signal))
    }
  }

}
```

4 Make your classes extend Resettable
-------------------------------------

Census uses the Resettable interface to pass the variables of each request to
each vertex and edge, and to reset the state of all the vertices and edges
before each computation. The method `reset (vars: Array[String])` receives the
array of variables sent in the json of the request.

```scala
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
    // If this vertex is the source vertex
    if (vertexId == variables(0)) state = Some(0)
    else state = None
    // Remember to reset the lastSignalState always
    lastSignalState = None
  }

  override def scoreSignal: Double = {
    if (state.isDefined && (!lastSignalState.isDefined || !lastSignalState.get.isDefined || state.get != lastSignalState.get.get)) 1.0
    else 0.0
  }
}
```

5 Make an object module which extends from a Graph class
--------------------------------------------------------

Census manages all the data importation and gives you an interface to interact
properly with the instantiation of your Signal/Collect classes and to start your
computation, all of that is made through a "Graph class" implementation, in this
case we will use an `UndirectedGraph` importation.

```scala
object SSCloseness extends UndirectedGraph {

}
```

The `UndirectedGraph` class as well as all the Census graph classes need to
concrete the methods which will create vertices and edges for the importation
process.

```scala
object SSCloseness extends UndirectedGraph {

  def vertex (id: Any) = new Location(id)

  def edge (target: Any) = new Path(target)

}
```

Also we need to concrete the `computeExecute (request: EngineComputeRequest, vars: Array[String]): Unit`
method of the graph class.

```scala
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
}
```

This method will be called when a computation request is called, you can use the
`graph` member which already has a Signal/Collect graph initiated to start the
Signal/Collect computation. When the computation is finished you can use the
`Database` object to reinsert the generated data back to the Neo4j database.

6 Add your new algorithm to the Library
---------------------------------------

The `Library` object verifies that an algorithm exists, so lets open the
`app/library/Library.scala` document and add our algorithm.

```scala
object Library {

  def apply (algorithm: String): Option[Graph] = {
    algorithm match {
      case "SSCloseness" => Some(SSCloseness)
      case "BrandesBetweenness" => Some(BrandesBetweenness)
      case "DirectedSSSP" => Some(DirectedSSSP)
      case "PageRank" => Some(PageRank)
      case _ => None
    }
  }

}
```

Now everything should be ready for a http request :D :tada:

Here is the complete `app/library/SSCloseness.scala` document.

```scala
package library

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
```
