///**
// * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
// */
//
//package compute.library
//
//import scala.collection.mutable.Map
//import scala.collection.mutable.Stack
//import scala.collection.mutable.ArrayBuffer
//import scala.collection.mutable.MutableList
//
//import play.api.libs.json._
//import play.api.libs.concurrent.Execution.Implicits._
//
//import com.signalcollect._
//import com.signalcollect.configuration.ExecutionMode
//
//import controllers.Neo4j
//import controllers.OutReports
//import controllers.requests.ComputeRequest
//import compute.GraphAlgorithm
//
//object BrandesStack extends Stack[BrandesVertex]
//
///**
// * Module that computes the Freeman's centrality 'Closeness' for
// * a single source vertex. It uses the Single Source Shortest Path 
// * algorithm for the sum of shortest paths.
// */
//object BrandesBetweenness extends GraphAlgorithm[BrandesVertex, BrandesEdge] {
//
//  /** The vertex id of which the centrality will be computed */
//  var source: String = ""
//
//  /** Array to update nodes with betweenness */
//  val reportNodes: ArrayBuffer[BrandesVertex] = ArrayBuffer()
//
//  /**
//   * Computation Methods.
//   */
//
//  /** 
//   * Starts the algorithm.
//   */
//  def computationExecute (computationRequest: ComputeRequest): Unit = {
//    // Compute.
//    computationRequest.stats = graph.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.Synchronous)).toString
//    graph.shutdown
//    // Brandes calculation.
//    val n = vertices.size
//    while (!BrandesStack.isEmpty) {
//      val w = BrandesStack.pop
//      for (v <- w.predecessors) v.delta = v.delta + (v.sigma.toDouble / w.sigma) * (1 + w.delta)
//      if (w.id != source) {
//        // Normalization.
//        w.delta = w.delta / ((n-2)*(n-1))
//        report(w)
//      }
//    }
//    report(computationRequest) 
//  }
//
//  /**
//   * Inserts the result back into the active Neo4j database.
//   *
//   * @param result of the closeness centrality computation.
//   */
//  private def report (w: BrandesVertex): Unit = {
//    if (w.delta <= 0) return
//    reportNodes += w 
//  }
//
//  private def report (computationRequest: ComputeRequest): Unit = {
//    var q = "MATCH "
//    for (w <- reportNodes) {
//      q = q+s"(a${w.id}:${importRequest.tag} {id:'${w.id}'})"
//      if (reportNodes.last == w) q=q+" "
//      else q=q+", " 
//    }
//    q=q+"SET "
//    for (w <- reportNodes) {
//      q = q+s"a${w.id}.betweenness = a${w.id}.betweenness + ${w.delta}"
//      if (reportNodes.last == w) q=q+" "
//      else q=q+", " 
//    }
//    reportNodes.clear
//    Neo4j.query(q) map { response =>
//      // Finish.
//    } recover { case _ =>
//      graph.shutdown
//      computationRequest.computationTime = System.currentTimeMillis - computationRequest.computationTime
//      OutReports.Error.unreachableNeo4j(importRequest)
//      computationRequest.finish()
//    }
//  }
//
//  /**
//   * Request Settings Methods.
//   */
//  
//  /**
//   * Validates the received vars from the ComputeRequest json,
//   * needed to know the source from which the centrality will be
//   * calculated.
//   *
//   * @param json to be validated.
//   * @return 'true' if it was successful.
//   *         'false' if there was an error.
//   */
//  def validateVars (json: JsValue): Boolean = {
//    (json \ "source").asOpt[String] match {
//      case None => return false
//      case Some(data) =>
//        source = data
//        return true
//    }
//  }
//
//  /**
//   * Used to set the signalcollect's graph by adding all the 
//   * imported vertices and edges before every computation.
//   */
//  def reset: Unit = {
//    BrandesStack.clear
//    vertices.foreach { case (key: String, vertex: BrandesVertex) =>
//      if (key == source) {
//        vertex.reset(Some(0))
//      } else {
//        vertex.reset()
//      }
//      graph.addVertex(vertex)
//    }
//    edges.foreach { case (key: String, array: ArrayBuffer[BrandesEdge]) =>
//      for (edge <- array) graph.addEdge(key, edge)
//    }
//  }
//  
//  /**
//   * Graph Import Methods.
//   */ 
//
//  /**
//   * Used to start the graph import for the algorithm.
//   */
//  def importStart: Unit = batch
//
//  /** 
//   * Queries Neo4j for a batch of 1000 vertices and it's
//   * relationships.
//   */
//  private def batch: Unit = {
//    Neo4j.query(
//      s"MATCH (a:${importRequest.tag}) "
//      + "WHERE not(has(a.censusimportbrandesbetweenness)) "
//      + "WITH a LIMIT 1000 "
//      + s"MATCH (a)--(b:${importRequest.tag}) "
//      + "SET a.censusimportbrandesbetweenness=true "
//      + "RETURN a.id, b.id"
//    ) map { res => 
//      validateJson(res.json)
//    } recover { case _ => 
//      OutReports.Error.unreachableNeo4j(importRequest)
//      importFinished(false)
//    }
//  }
//
//  /**
//   * Validates the response from a batch, reports any format error
//   * and starts the next batch or cleans the Neo4j database from
//   * the import to finish the request.
//   *
//   * @param json to be validated.
//   */
//  private def validateJson (json: JsValue): Unit = {
//    (json \ "data").asOpt[Array[Array[String]]] match {
//      case Some(data) => 
//        // Keep importing arrays until an empty one is received.
//        if (importArray(data)) {
//          batch
//        } else {
//          clearDatabase
//          importFinished(true)
//        }
//      case None => 
//        importFinished(false)
//    }
//  }
//
//  /**
//   * Imports an already parsed json response from Neo4j, this method
//   * uses the abstract method 'insertTriple'. If it receives an empty
//   * array, it assumes that there are no more nodes to import.
//   *
//   * @param data the parsed array with the triplets from Neo4j.
//   * @return 'true' if there is still more batch imports to do.
//   *         'false' if there are no more batch imports to do.
//   */
//  private def importArray (data: Array[Array[String]]): Boolean = {
//    if (!data.isEmpty) {
//      for (rel: Array[String] <- data) {
//        insertTriple(rel(0), rel(1))
//      }
//      return true
//    } else {
//      return false
//    }
//  }
//
//  /**
//   * Called every time a triplet is received from Neo4j.
//   *
//   * @param aID the id of the source vertex.
//   * @param bID the id of the related vertex.
//   */
//  private def insertTriple (aID: String, bID: String): Unit = {
//    vertices.getOrElseUpdate(aID, new BrandesVertex(aID))
//    vertices.getOrElseUpdate(bID, new BrandesVertex(bID))
//    val array = edges.getOrElseUpdate(aID, ArrayBuffer[BrandesEdge]())
//    array += new BrandesEdge(bID)
//  }
//
//  /**
//   * Removes the tag on neo4j nodes used for the import.
//   */
//  private def clearDatabase: Unit = {
//    Neo4j.query(s"MATCH (n:${importRequest.tag} {censusimportbrandesbetweenness:true}) REMOVE n.censusimportbrandesbetweenness") recover {
//      // Report: Neo4j server unreachable.
//      case _ => OutReports.Error.unreachableNeo4j(importRequest)
//    }
//  }
//
//}
//
//class BrandesEdge(t: String) extends OptionalSignalEdge(t) {
//  def signal = {
//    source.state match {
//      case None => None
//      case Some(distance: Int) => Some((distance + weight.toInt, source))
//    }
//  }
//}
//
//class BrandesVertex(vertexId: String, initialState: Option[Int] = None) extends DataFlowVertex(vertexId, initialState) {
//  type Signal = Tuple2[Int, BrandesVertex]
//
//  var sigma: Int = 0
//  var delta: Double = 0.0
//  val predecessors = MutableList[BrandesVertex]()
//
//  def reset (s:Option[Int] = None): Unit = {
//    state = s
//    sigma = 0
//    delta = 0.0
//    predecessors.clear
//    lastSignalState = None
//    if (s == Some(0)) {
//      sigma = 1
//      BrandesStack.push(this)
//    }
//  }
//
//  def collect(signal: Tuple2[Int, BrandesVertex]) = {
//    val source = signal._2
//    val signalState = signal._1
//    val sourceState = source.state.get
//    val newState = state match {
//      case None => 
//        BrandesStack.push(this)
//        Some(signalState)
//      case Some(currentShortestPath) => 
//        Some(math.min(currentShortestPath, signalState))
//    }
//    if (sourceState + 1 == newState.get) {
//      sigma = sigma + source.sigma
//      predecessors += source
//    }
//    newState
//  }
//
//  override def scoreSignal: Double = {
//    if (state.isDefined && (!lastSignalState.isDefined || !lastSignalState.get.isDefined || state.get != lastSignalState.get.get)) {
//      1.0
//    } else {
//      0.0
//    }
//  }
//
//}
