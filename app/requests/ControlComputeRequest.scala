/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers.requests

import scala.concurrent._

import play.api.libs.json._

import library.Library
import shared.Neo4j
import shared.Utils

/**
 * An in queue request that executes a
 * graph algorithm in the library.
 *
 * @param json of the request.
 */
class ControlComputeRequest (json: JsValue) extends Request {

  /** A unique identifier for this request. */
  val token: String = Utils.genUUID

  /** Algorithm to be executed. */
  val algorithm: String =
    (json \ "algorithm").asOpt[String] match {
      case None => errors += "'algorithm' field missing."; ""
      case Some(data) => Library(data, this) match {
        case None => errors += s"No such algorithm '$data'"; ""
        case Some(algo) => data
      }
    }

  val local: Boolean =
    (json \ "local").asOpt[Boolean] match {
      case None => false
      case Some(data) => data
    }

  /** Size of the orchestration, only used for MultiNodeRequests. */
  val numberOfInstances: Int =
    if (local) 1
    else
      (json \ "instances").asOpt[Int] match {
        case None => 1
        case Some(data) => data
      }

  val bulk: String = 
    (json \ "bulk").asOpt[String] match {
      case None => "singlet"
      case Some(data) => data match {
        case "all-pair" => "all-pair"
        case "all-sources" => "all-sources"
        case _ => errors += s"No such bulk type: $data"; ""
      }
    }

  /** Variables that the algorithm will use for the computation. */
  val vars: Array[String] =
    (json \ "vars").asOpt[Array[String]] match {
      case None => Array[String]()
      case Some(data) => data
    }

  /** Tag used for the Neo4j importation. */
  val dbTag: String =
    (json \ "graph" \ "tag").asOpt[String] match {
      case None => errors += "'tag' field missing."; ""
      case Some(data) => data
    }

  /** Neo4j database host. */
  private val dbHost: String = 
    (json \ "graph" \ "host").asOpt[String] match {
      case None => errors += "'host' field missing."; ""
      case Some(data) => data replaceAll ("http://", "")
    }

  /** Neo4j database port. */
  private val dbPort: Int =
    (json \ "graph" \ "port").asOpt[Int] match {
      case None => errors += "'port' field missing."; ""
      case Some(data) => data
    }

  /** Neo4j database username. */
  private val dbUser: String =
    (json \ "graph" \ "user").asOpt[String] match {
      case None => null
      case Some(data) => data
    }

  /** Neo4j database password. */
  private val dbPass: String = 
    (json \ "graph" \ "password").asOpt[String] match {
      case None => null
      case Some(data) => data
    }

  /** Neo4j database that will be used for the node importation. */
  val database: Neo4j = new Neo4j(dbHost, dbPort, dbUser, dbPass)

}
