/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import scala.concurrent._

import play.api.libs.json._

import library.Library
import shared.Neo4j
import shared.Utils

/** Verifies and encapsulates all the parameters of a computation request to Census Control.
  *
  * A json example with all possible parameters. (note that some parameters may override others):
  {{{
{
  "algorithm": "SSCloseness",
  "instances": 1,
  "bulk": "singlet",
  "vars": ["sourceid"],
  "engines": [
    {"server-ip": "10.124.23.1",
     "server-port": 9000},
    {"server-ip": "10.124.23.2",
     "server-port": 9000}
  ]
  "graph": {
    "tag": "Person",
    "host": "http://test.graphenedb.com/",
    "port": 24789,
    "user": "root",
    "password": "admin"
  }
}
  }}}
  * And here the public data structure attributes that has the request:
  {{{
class ControlComputeRequest {
  val token: String
  val algorithm: String
  val numberOfInstances: Int
  val engines: List[Tuple2[String, Int]]
  val vars: Array[String]
  val dbTag: String
  val database: Neo4j
}
  }}}
  *
  * @constructor creates a data structure with all the request's parameters.
  * @param json of the request.
  */
class ControlComputeRequest (json: JsValue) extends Request {

  /** A unique identifier for this request. */
  val token: String = Utils.genUUID

  /** Algorithm name to be executed. */
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

  /** Amount of GCE instances to be created, (Custom Census Engine servers have priority). */
  val numberOfInstances: Int =
    (json \ "instances").asOpt[Int] match {
      case None => 1
      case Some(data) => data
    }

  /** IPs of the custom Census Engine servers. */
  private val customIPs =
    (json \ "engines" \\ "server-ip") map { obj => 
      obj.asOpt[String] match {
        case None => 
          errors += "'server-host' field must be a String."
          null
        case Some(data) => data
      }
    }

  /** Ports of the custom Census Engine servers. */
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

  /** Zip of the customIPs and customPosts. */
  val engines = (customIPs zip customPorts)

  /** The user can decide for which nodes to compute the algorithm.
    *
    * That is set in this parameter, for example:
    *
    * singlet: to compute for only 1 node.
    * all-sources: to compute for every node.
    * all-pairs: to compute for every couple of nodes.
    */
  val bulk: String = 
    (json \ "bulk").asOpt[String] match {
      case None => "singlet"
      case Some(data) => data match {
        case "singlet" => data
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
