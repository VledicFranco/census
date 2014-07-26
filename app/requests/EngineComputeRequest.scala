/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import play.api.libs.json._

import library.Library
import engine.Graph

/** Verifies and encapsulates all the parameters of a computation request to a Census Engine instance.
  *
  * A json example with all possible parameters:
  {{{
{
  "token": "asdw-12d24-awdqsr1-qwed2",
  "algorithm": "SSCloseness",
  "vars": ["sourceid"]
}
  }}}
  * And here the public data structure attributes that has the request:
  {{{
class ControlComputeRequest {
  val token: String
  val algorithm: Graph
  val vars: Array[String]
}
  }}}
  *
  * @constructor creates a data structure with all the request's parameters.
  * @param json of the request.
  */
class EngineComputeRequest (json: JsValue) extends Request {

  /** The request uniquer identifier. */
  val token: String = 
    (json \ "token").asOpt[String] match {
      case None => 
        errors += "'token' field missing."
        null
      case Some(data) => data
    }

  /** Algorithm to be executed. */
  val algorithm: Graph =
    (json \ "algorithm").asOpt[String] match {
      case None => 
        errors += "'algorithm' field missing."
        null
      case Some(data) => Library(data) match {
        case None => 
          errors += s"No such algorithm '$data'"
          null
        case Some(algo) => algo
      }
    }

  /** Variables that the algorithm will use for the computation. */
  val vars: Array[String] =
    (json \ "vars").asOpt[Array[String]] match {
      case None => Array[String]()
      case Some(data) => data
    }

}
