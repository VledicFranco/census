/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import scala.concurrent._

import play.api.libs.json._

import library.Library
import shared.Neo4j
import shared.Utils

/**
{
  "algorithm": "SSCloseness",
  "instances": 1,
  "bulk": "singlet",
  "vars": ["sourceid"],
  "graph": {
    "tag": "Profile",
    "host": "http://spribo2.sb01.stations.graphenedb.com/",
    "port": 24789,
    "user": "spribo2",
    "password": "6e0mtjm8OEgoSRpjNhii"
  }
}
 *
 * @param json of the request.
 */
class ControlComputeRequest (json: JsValue) extends Request {

  /** A unique identifier for this request. */
  val token: String = Utils.genUUID

  /** Algorithm to be executed. */
  val algorithm: String =
    (json \ "algorithm").asOpt[String] match {
      case None => 
        errors += "'algorithm' field missing."
        null
      case Some(data) => Library(data) match {
        case None => 
          errors += s"No such algorithm '$data'"
          null
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
    (json \ "instances").asOpt[Int] match {
      case None => 1
      case Some(data) => data
    }

  private val customIPs =
    (json \ "engines" \\ "server-ip") map { obj => 
      obj.asOpt[String] match {
        case None => 
          errors += "'server-host' field must be a String."
          null
        case Some(data) => data
      }
    }

  private val customPorts =
    (json \ "engines" \\ "server-port") map { obj => 
      obj.asOpt[Int] match {
        case None =>
          errors += "'server-port' field must be a Int."
          0
        case Some(data) => data
      }
    }

  if (customIPs.size > customPorts.size)
    errors += "Missing ports for engine servers."
  if (customPorts.size > customIPs.size)
    errors += "Missing IPs for engine servers."

  val engines = (customIPs zip customPorts)

  val bulk: String = 
    (json \ "bulk").asOpt[String] match {
      case None => "singlet"
      case Some(data) => data match {
        case "singlet" => data
        case "all-pair" => data
        case "all-sources" => data
        case _ => 
          errors += s"No such bulk type: $data"
          null
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
      case None => 
        errors += "'tag' field missing."
        null
      case Some(data) => data
    }

  /** Neo4j database host. */
  private val dbHost: String = 
    (json \ "graph" \ "host").asOpt[String] match {
      case None => 
        errors += "'host' field missing."
        null
      case Some(data) => (data replaceAll ("http://", "")) replaceAll ("/", "")
    }

  /** Neo4j database port. */
  private val dbPort: Int =
    (json \ "graph" \ "port").asOpt[Int] match {
      case None => 
        errors += "'port' field missing."
        0
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
