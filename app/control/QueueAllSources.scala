/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.collection.mutable.Queue

import http.OutReports
import requests.ControlComputeRequest

trait QueueAllSources extends QueueFiller {

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
