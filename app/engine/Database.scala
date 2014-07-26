/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package engine

import shared.Neo4j

import play.api.libs.ws.Response

/** Neo4j singleton for Census Engine. */
object Database {

  /** Tag used to identify the working nodes. */
  var tag: String = ""

  /** [[shared.Neo4j]] instance to be used. */
  private var database: Neo4j = null

  /** Creates a new [[shared.Neo4j]] instance.
    *
    * @param host of the neo4j server.
    * @param port of the neo4j server.
    * @param user of the neo4j server.
    * @param password of the neo4j server.
    */
  def setDatabase (host: String, port: Int, user: String, password: String): Unit =
    database = new Neo4j(host, port, user, password)

  /** Queries through http to the registered Neo4j server.
    *
    * @param query string (cypher).
    * @param callback (Response, Boolean)=>Unit callback with 
    *                 second parameter 'true' if there was an error.
    */
  def query (querystring: String, callback: (Boolean, Response)=>Unit): Unit = 
    database.query(querystring, callback)

  /** Makes a request to the web service with HEAD method and a timeout to test reachability.
    *
    * @param callback Boolean=>Unit callback with parameter 
    *                 'true' if ping was successful.
    */
  def ping (callback: Boolean=>Unit): Unit = 
    database.ping(callback)

}
