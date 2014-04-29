/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import scala.concurrent._

import play.api.libs.concurrent.Execution.Implicits._

import controllers.N4j
import controllers.WebService
import compute.EngineAlgorithm

class Instance (val h: String, val p: Int) extends WebService {

  this.setHost(host, port)
  
  var activeGraphHost: N4j = null

  var activeAlgorithm: String = null

  /** Queue for the requests. */
  var queue: Array[EngineAlgorithm] = new Array[EngineAlgorithm](conf.ce_max_queue_size)

  def hasFreeSpace: Boolean = {
    for (algo: EngineAlgorithm <- queue) {
      if (algo == null)
        return true
    }
    return false
  }

  def hasEngineAlgorithmRequest (token: String): Boolean = {
    for (algo: EngineAlgorithm <- queue) {
      if (algo != null && algo.token == token)
        return true
    }
    return false
  }

  //def enqueue 

  //def finishedEngineAlgorithmRequest

}
