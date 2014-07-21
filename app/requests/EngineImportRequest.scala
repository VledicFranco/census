/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import play.api.libs.json._

import http.OutReports
import library.Library
import engine.GraphImport
import engine.DB

/**
 * An in queue request that imports a graph
 * from DB with an algorithm format.
 *
 * @param json of the request.
 */
class EngineImportRequest (json: JsValue) extends Request {

  /** Unique identifier for the request. */
  val token =
    (json \ "token").asOpt[String] match {
      case None => errors += "'token' field missing."
      case Some(data) => token = data
    }

  /** The algorithm that will format and import the graph. */
  val graph: GraphImport =
    (json \ "algorithm").asOpt[String] match {
      case None => errors += "'algorithm' field missing."; ""
      case Some(data) => Library(data) match {
        case Some(algo) => graph = algo
        case None => errors += s"No such algorithm '$data'"; ""
      }
    }

  /** Extra cypher to be used for the graph importation. */
  val tag: String = null
    (json \ "tag").asOpt[String] match {
      case None => errors += "'tag' field missing."; ""
      case Some(data) => tag = data
    }

  /** The DB server hostname. */
  val host: String = 
    (json \ "host").asOpt[String] match {
      case None => errors += "'host' field missing."; ""
      case Some(data) => data
    }

  /** The DB server port. */
  val port: Int = 
    (json \ "port").asOpt[Int] match {
      case None => errors += "'port' field missing."; 0
      case Some(data) => data
    }

  /** The DB server username. */
  val user: String = 
    (json \ "user").asOpt[String] match {
      case None => null
      case Some(data) => data
    }

  /** The DB server password. */
  val password: String = 
    (json \ "password").asOpt[String] match {
      case None => null
      case Some(data) => data
    }

}
