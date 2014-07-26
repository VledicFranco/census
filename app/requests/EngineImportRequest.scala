/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import play.api.libs.json._

import engine.Graph
import library.Library

/** Verifies and encapsulates all the parameters of a graph importation request to a Census Engine instance.
  *
  * A json example with all possible parameters:
  {{{
{
  "token": "asdw-12d24-awdqsr1-qwed2",
  "algorithm": "SSCloseness",
  "tag": "Person",
  "host": "http://test.graphenedb.com/",
  "port": 24789,
  "user": "root",
  "password": "admin"
}
  }}}
  * And here the public data structure attributes that has the request:
  {{{
class ControlComputeRequest {
  val token: String
  val algorithm: Graph
  val tag: String
  val host: String
  val port: Int
  val user: String
  val password: String
}
  }}}
  *
  * @constructor creates a data structure with all the request's parameters.
  * @param json of the request.
  */
class EngineImportRequest (json: JsValue) extends Request {

  /** Unique identifier for the request. */
  val token =
    (json \ "token").asOpt[String] match {
      case None => 
        errors += "'token' field missing."
        null
      case Some(data) => data
    }

  /** The algorithm that will format and import the graph. */
  val graph: Graph =
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

  /** Extra cypher to be used for the graph importation. */
  val tag: String =
    (json \ "tag").asOpt[String] match {
      case None => 
        errors += "'tag' field missing."
        null
      case Some(data) => data
    }

  /** The Database server hostname. */
  val host: String = 
    (json \ "host").asOpt[String] match {
      case None => 
        errors += "'host' field missing."
        null
      case Some(data) => data
    }

  /** The Database server port. */
  val port: Int = 
    (json \ "port").asOpt[Int] match {
      case None => 
        errors += "'port' field missing."
        0
      case Some(data) => data
    }

  /** The Database server username. */
  val user: String = 
    (json \ "user").asOpt[String] match {
      case None => null
      case Some(data) => data
    }

  /** The Database server password. */
  val password: String = 
    (json \ "password").asOpt[String] match {
      case None => null
      case Some(data) => data
    }

}
