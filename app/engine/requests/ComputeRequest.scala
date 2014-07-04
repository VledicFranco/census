/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers.requests

import play.api.libs.json._

import compute.{Library, GraphCompute}

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
    req
  }

}

/**
 * An in queue request that executes a
 * graph algorithm in the library.
 *
 * @param json of the request.
 */
class ComputeRequest (json: JsValue) extends QueueRequest {

  /** Token to identify the request. */
  var token: String = null

  /** Algorithm to be executed. */
  var algorithm: GraphCompute = null

  /** Moment when the request was created. */
  var creationTime: Long = 0

  /** Variables that the algorithm will use for the computation. */
  var vars: Array[String] = null

  /** The Signal Collect stats of the computation. */
  var stats: String = null

  /** The amount of milliseconds that the computation took. */
  var computationTime: Long = 0

  /**
   * Json validation.
   */
  def validate: Unit = {
    (json \ "token").asOpt[String] match {
      case None => errors = errors :+ "'token' field missing."
      case Some(data) => token = data
    }
    (json \ "algorithm").asOpt[String] match {
      case None => errors = errors :+ "'algorithm' field missing."
      case Some(data) => Library(data) match {
        case None => errors = errors :+ s"No such algorithm '$data'"
        case Some(algo) => algorithm = algo
      }
    }
    (json \ "creationTime").asOpt[Long] match {
      case None => errors = errors :+ "'creationTime' field missing."
      case Some(data) => creationTime = data
    }
    vars = (json \ "vars").as[Array[String]]
  }

  /**
   * Request execution.
   */
  def execute: Unit = algorithm.computeStart(this, vars)

}
