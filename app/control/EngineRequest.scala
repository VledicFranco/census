/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control 

import play.api.libs.json._
import shared.Utils
import shared.Neo4j

trait EngineRequest {
  
  val token = Utils.genUUID

  val payload: JsValue

}

class ImportRequest (algorithm: String, tag: String, database: Neo4j) extends EngineRequest {

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

class ComputeRequest (algorithm: String, vars: Array[String] = null) extends EngineRequest {

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
