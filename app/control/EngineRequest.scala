/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control 

import play.api.libs.json._
import shared.Utils
import shared.Neo4j

/** A wraper for census engine requests. */
trait EngineRequest {

  /** A unique identifier for this request. */
  val token = Utils.genUUID

  /** The json data to be sent. */
  val payload: JsValue

}

/** A census engine graph import request wraper.
  * 
  * @constructor create a new import request with a formated json payload.
  * @param algorithm to format the graph for.
  * @param tag to be used in the import.
  * @param database to import from.
  */
class ImportRequest (algorithm: String, tag: String, database: Neo4j) extends EngineRequest {

  /** Json to be sent in the request. */
  val payload: JsValue = 
    if (database.user == null)  
      Json.obj(
        "token" -> token,
        "algorithm" -> algorithm,
        "tag" -> tag,
        "host" -> database.host,
        "port" -> database.port
      )
    else
      Json.obj(
        "token" -> token,
        "algorithm" -> algorithm,
        "tag" -> tag,
        "host" -> database.host,
        "port" -> database.port,
        "user" -> database.user,
        "password" -> database.password
      )

}

/** A census engine compute request wraper.
  * 
  * @constructor create a new compute request with a formated json payload.
  * @param algorithm to be computed.
  * @param vars to be used in the algorithm computation.
  */
class ComputeRequest (algorithm: String, vars: Array[String] = null) extends EngineRequest {

  /** Json to be sent in the request. */
  val payload: JsValue = 
    if (vars == null)
      Json.obj(
        "token" -> token,
        "algorithm" -> algorithm
      )
    else 
      Json.obj(
        "token" -> token,
        "algorithm" -> algorithm,
        "vars" -> Json.toJson(vars)
      )

}
