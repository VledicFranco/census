/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import scala.collection.mutable.ArrayBuffer

import com.signalcollect._

import controllers.{Neo4j, OutReports}
import controllers.requests.ImportRequest

/**
 * Used to import the basic topology of a Neo4j graph.
 */
trait UndirectedGraphImport extends GraphImport {

  /**
   * Creates a new vertex to add it to the vertices
   * data structure.
   *
   * @param id of the new vertex.
   * @returns a new vertex.
   */
  def vertex (id: Any): Vertex[Any, _]

  /**
   * Creates a new edge to add it to the edges
   * data structure.
   *
   * @param target id which the edge points to.
   * @returns a new edge.
   */
  def edge (target: Any): Edge[Any]

  /** 
   * Queries Neo4j for a batch of 1000 vertices and it's
   * relationships.
   */
  def importExecute (importRequest: ImportRequest): Unit = {
    val importId = "census"+importRequest.token.split("-").last
    val batchQuery = (
       s"MATCH (a:${importRequest.tag}) "
      +s"WHERE not(has(a.$importId)) "
      + "WITH a LIMIT 1000 "
      +s"MATCH (a)--(b:${importRequest.tag}) "
      +s"SET a.$importId=true "
      + "RETURN a.id, b.id")
    Neo4j.query(batchQuery, { (response, error) =>
      if (error) {
        clearDatabase(importRequest)
        importFinish(importRequest, false)
        return
      }
      val data = (response.json \ "data").as[Array[Array[String]]]
      if (!data.isEmpty) {
        for (relation <- data) triple(relation(0), relation(1))
        importExecute(importRequest)
      } else {
        clearDatabase(importRequest)
        importFinish(importRequest, true)
      }
    })
  }

  /**
   * Adds without repeating two vertices and a relation
   * between them to the data structures.
   *
   * @param a id of the first vertex.
   * @param b id of the second vertex.
   */
  def triple (a: Any, b: Any): Unit = {
    vertices.getOrElseUpdate(a, vertex(a))
    vertices.getOrElseUpdate(b, vertex(b))
    edges.getOrElseUpdate(a, ArrayBuffer[Edge[Any]]()) += edge(b)
  }

  /**
   * Removes the tag on neo4j nodes used for the import.
   *
   * @param importRequest used for this import.
   */
  def clearDatabase (importRequest: ImportRequest): Unit = {
    val importId = "census"+importRequest.token.split("-").last
    val clearQuery = (
      s"MATCH (a:${importRequest.tag} {$importId:true}) "
     +s"REMOVE a.$importId")
    Neo4j.query(clearQuery, { (response, error) =>
      if (error) OutReports.Error.unreachableNeo4j(importRequest)
    })
  }

}
