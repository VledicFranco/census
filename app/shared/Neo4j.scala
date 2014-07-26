/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package shared

import play.api.libs.ws.Response
import play.api.libs.json._

/** Class that handles the Neo4j http queries. 
  *
  * @constructor creates an object that can query to a Neo4j server.
  * @param host of the neo4j server.
  * @param port of the neo4j server.
  * @param user of the neo4j server.
  * @param password of the neo4j server.
  */
class Neo4j (
  val host: String, 
  val port: Int, 
  val user: String = null, 
  val password: String = null) 
extends WebService {

  /** Queries through http to the registered Neo4j server.
    *
    * @param query string (cypher).
    * @param callback (Response, Boolean)=>Unit callback with 
    *                 second parameter 'true' if there was an error.
    */
  def query (query: String, callback: (Boolean, Response)=>Unit): Unit = {
    post("/db/data/cypher", Json.obj("query" -> query), callback)   
  }

}
