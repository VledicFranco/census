/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import scala.concurrent.Future
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Module that handles the Neo4j http queries.
 */
class N4j extends WebService {

  var tag: String = null

  /**
   * Queries through http to the registered Neo4j
   * server.
   *
   * @param query string (cypher).
   * @return a future that handles the response.
   */
  def query (query: String): Future[Response] = {
    post("/db/data/cypher", s"""{"query": "$query"}""")
  }

}
