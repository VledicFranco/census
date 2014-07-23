/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.collection.mutable.Queue

import http.OutReports
import requests.ControlComputeRequest

trait QueueAllSources extends QueueFiller {

  protected val requestsQueue = Queue[EngineRequest]() 

  private def queryAllIdsWithRelations (request: ControlComputeRequest): Unit = {
    val tag = request.dbTag
    val database = request.database
    database.query(s"MATCH (a:$tag)--(b:$tag) RETURN distinct a.id", { (error, response) =>
      if (error)
        return OutReports.Error.unreachableNeo4j(request)
      val data = (response.json \ "data").as[Array[String]]
      if (!data.isEmpty) {
        for (source <- data) 
          requestsQueue.enqueue(new ComputeRequest(request.algorithm, Array(source)))
        fillingFinished
      }
    })
  }

  def fillQueue (request: ControlComputeRequest): Unit = queryAllIdsWithRelations(request)

}
