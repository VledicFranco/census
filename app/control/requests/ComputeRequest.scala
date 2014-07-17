/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers.requests

import scala.concurrent._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import instances.conf
import controllers.HTTPHook
import shared.Neo4j
import compute.Library
import compute.Receiver

/**
 * Companion object to correctly build the request.
 */
object ComputeRequest {

  /**
   * Constructor that creates the request, and executes
   * the validation.
   *
   * @param json of the request.
   * @return a request instance.
   */
  def apply (json: JsValue): ComputeRequest = {
    val req = new ComputeRequest(json)
    req.validate
    req.execute
    req
  }

}

/**
 * An in queue request that executes a
 * graph algorithm in the library.
 *
 * @param json of the request.
 */
class ComputeRequest (json: JsValue) extends Request {

  /** Algorithm to be executed. */
  var algorithm: Receiver = null

  /** Size of the orchestration, only used for MultiNodeRequests. */
  var numberOfInstances: Int = 0

  /** Moment when the request was created. */
  var creationTime: Long = 0

  /** Tag used for the Neo4j importation. */
  var tag: String = null

  /** Neo4j database that will be used for the node importation. */
  var database: Neo4j = null

  /** Neo4j database host. */
  var n4jhost: String = null

  /** Neo4j database port. */
  var n4jport: Int = 0

  /** Neo4j database username. */
  var n4juser: String = null

  /** Neo4j database password. */
  var n4jpassword: String = null

  /** The amount of milliseconds that the computation took. */
  var computationTime: Long = 0

  /**
   * Json validation.
   */
  def validate: Unit = {
    (json \ "algorithm").asOpt[String] match {
      case None => errors = errors :+ "'algorithm' field missing."
      case Some(data) => Library(data, this) match {
        case None => errors = errors :+ s"No such algorithm '$data'"
        case Some(algo) => algorithm = algo
      }
    }
    (json \ "instances").asOpt[Int] match {
      case None => numberOfInstances = conf.min_instances
      case Some(number) => numberOfInstances = number
    }
    (json \ "graph" \ "tag").asOpt[String] match {
      case None => errors = errors :+ "'tag' field missing."
      case Some(data) => tag = data
    }
    (json \ "graph" \ "host").asOpt[String] match {
      case None => errors = errors :+ "'host' field missing."
      case Some(data) => n4jhost = data
    }
    (json \ "graph" \ "port").asOpt[Int] match {
      case None => errors = errors :+ "'port' field missing."
      case Some(data) => n4jport = data
    }
    (json \ "graph" \ "user").asOpt[String] match {
      case None =>
      case Some(data) => n4juser = data
    }
    (json \ "graph" \ "password").asOpt[String] match {
      case None => 
      case Some(data) => n4jpassword = data
    }
  }

  /**
   * Request execution.
   */
  def start: Unit = {
    database = new Neo4j
    database.tag = tag
    database.setHost(n4jhost, n4jport)
    database.setAuth(n4juser, n4jpassword)
    // Check for server connectivity.
    database.ping map { response => 
      algorithm.receive
    } recover {
      case _ => 
        // Report: Neo4j server unreachable.
        HTTPHook.Error.unreachableNeo4j(this)
    }
  }

}
