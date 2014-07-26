/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.collection.mutable.Queue

import http.OutReports
import requests.ControlComputeRequest

/** Fills the queue with every node id which has at least 1
  * relationship to other node with the same tag.
  */
trait QueueAllSources extends QueueFiller {

  /** Queries the database for all the node sources, then it fills the queue,
    * in the end it calls 'fillingFinished'.
    *
    * @param request with all the needed data for the filling.
    */
  def fillQueue (request: ControlComputeRequest): Unit = {
    val tag = request.dbTag
    val database = request.database
    database.query(s"MATCH (a:$tag)--(b:$tag) RETURN distinct a.id", { (error, response) =>
      if (error)
        OutReports.Error.unreachableNeo4j(request)
      else {
        val data = (response.json \ "data").as[Array[Array[String]]]
        for (source <- data)
          requestsQueue.enqueue(new ComputeRequest(request.algorithm, Array(source(0))))
        fillingFinished
      }
    })
  }

}
