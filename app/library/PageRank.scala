///**
// * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
// */
//
//package compute.library
//
//import scala.collection.mutable.ArrayBuffer
//
//import com.signalcollect._
//
//import compute.{UndirectedGraph, Resettable}
//import controllers.{Neo4j, OutReports}
//import controllers.requests.ComputeRequest
//
//object PageRank extends UndirectedGraph {
//
//  val reportNodes = ArrayBuffer[PageRankVertex]()
//
//  def vertex (id: Any) = new PageRankVertex(id)
//  def edge (target: Any) = new PageRankEdge(target)
//
//  def computeExecute (computationRequest: ComputeRequest, variables: Array[String]): Unit = {
//    computationRequest.stats = graph.execute.toString
//    graph.shutdown
//
//    vertices.foreach { case (id, vertex) =>
//      println(vertex)
//      //report(computationRequest, vertex)
//    }
//    //report(computationRequest)
//  }
//
//  class PageRankVertex(id: Any, baseRank: Double = 0.15) extends DataGraphVertex(id, baseRank) {
//    type Signal = Double
//    def dampingFactor = 1 - baseRank
//    def collect = baseRank + dampingFactor * signals.sum
//  }
//
//  class PageRankEdge(targetId: Any) extends DefaultEdge(targetId) {
//    type Source = PageRankVertex
//    def signal = source.state * weight / source.sumOfOutWeights
//  }
//
//  private def report (computationRequest: ComputeRequest, vertex: PageRankVertex): Unit = {
//    reportNodes += vertex 
//    if (reportNodes.size == 10)
//      report(computationRequest)
//  }
//
//  private def report (computationRequest: ComputeRequest): Unit = {
//    var q = "MATCH "
//    for (w <- reportNodes) {
//      q = q+s"(a${w.id} {id:'${w.id}'})"
//      if (reportNodes.last == w) q=q+" "
//      else q=q+", " 
//    }
//    q=q+"SET "
//    for (w <- reportNodes) {
//      q = q+s"a${w.id}.pagerank = ${w.state}"
//      if (reportNodes.last == w) q=q+" "
//      else q=q+", " 
//    }
//    reportNodes.clear
//    Neo4j.query(q, { (error, response) => 
//      if (error) println("NEO4J ERROR") 
//    })
//  }
//
//}
