/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import scala.concurrent._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import controllers.HTTPHook
import controllers.N4j
import compute.Library
import compute.EngineAlgorithm

/**
 * Companion object to correctly build the request.
 */
object ComputationRequest {

  /**
   * Constructor that creates the request, and executes
   * the validation.
   *
   * @param json of the request.
   * @return a request instance.
   */
  def apply (json: JsValue): ComputationRequest = {
    val req = new ComputationRequest(json)
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
class ComputationRequest (json: JsValue) extends Request {

  /** Algorithm to be executed. */
  var algorithm: EngineAlgorithm = null

  /** Moment when the request was created. */
  var creationTime: Long = 0

  var tag: String = null

  var database: N4j = null

  var n4jhost: String = null

  var n4jport: Int = 0

  var n4juser: String = null

  var n4jpassword: String = null

  /** The amount of milliseconds that the computation took. */
  var computationTime: Long = 0

  /**
   * Json validation.
   */
  override def validate: Unit = {
    (json \ "algorithm").asOpt[String] match {
      case None => errors = errors :+ "'algorithm' field missing."
      case Some(data) => Library(data, this) match {
        case None => errors = errors :+ s"No such algorithm '$data'"
        case Some(algo) => algorithm = algo
      }
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
  def execute: Unit = {
    if (errors.length > 0) return

    database = new N4j
    database.tag = tag
    database.setHost(n4jhost, n4jport)
    database.setAuth(n4juser, n4jpassword)
    
    // Check for server connectivity.
    database.ping map { response => 
      algorithm.enqueue
    } recover {
      case _ => 
        // Report: Neo4j server unreachable.
        HTTPHook.Error.unreachableN4j(this)
    }
  }

}
