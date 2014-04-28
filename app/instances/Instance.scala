/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import scala.concurrent._
import scala.collection.mutable.Queue

import play.api.libs.concurrent.Execution.Implicits._

import controllers.WebService
import compute.EngineAlgorithm

class Instance (val h: String, val p: Int) extends WebService {

  this.setHost(host, port)
  
  var activeGraphHost: String = null

  var activeAlgorithm: String = null

  /** Queue for the requests. */
  var queue: Queue[EngineAlgorithm] = Queue()

}
