/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import scala.concurrent.future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._

import library.Library
import engine.GraphCompute

/**
 * An in queue request that executes a
 * graph algorithm in the library.
 *
 * @param json of the request.
 */
class EngineComputeRequest (json: JsValue) extends Request {

  /** Algorithm to be executed. */
  var algorithm: GraphCompute = null

  /** Variables that the algorithm will use for the computation. */
  var vars: Array[String] = null

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
    vars = (json \ "vars").as[Array[String]]
  }

  /**
   * Request execution.
   */
  def body: Unit = future {algorithm.computeStart(this, vars)}

}
