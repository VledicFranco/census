/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package shared

import play.api.libs.ws.Response
import play.api.libs.json._

/**
 * Class that handles the Neo4j http queries.
 */
class Neo4j (
  val host: String, 
  val port: Int, 
  val user: String = null, 
  val password: String = null) 
extends WebService {

  /**
   * Queries through http to the registered Neo4j
   * server.
   *
   * @param query string (cypher).
   * @param callback (Response, Boolean)=>Unit callback with 
   *                 second parameter 'true' if there was an error.
   * @return a future that handles the response.
   */
  def query (query: String, callback: (Boolean, Response)=>Unit): Unit = {
    post("/db/data/cypher", Json.obj("query" -> query), callback)   
  }

}
