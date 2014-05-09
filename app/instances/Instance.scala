/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import scala.concurrent._

import com.github.nscala_time.time.Imports._ 

import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits._

import controllers.N4j
import controllers.WebService
import compute.EngineAlgorithm

class Instance (val h: String, val p: Int) extends WebService {

  setHost(h, p)

  var activeDatabase: N4j = null

  var activeAlgorithm: String = null

  /** Queue for the requests. */
  var queue: Array[EngineAlgorithm] = new Array[EngineAlgorithm](conf.ce_max_queue_size)

  def initialize (callback: Instance=>Unit): Unit = {
    ping map { response =>
      post("/control", "{"
        +s""" "host": "${conf.census_control_host}", """
        +s""" "port": ${conf.census_control_port} """
        + "}"
      ) map { res => callback(this) }
    } recover { case _ => 
      println(s"${DateTime.now} - INFO: Service $host still not ready, will wait 3 seconds.")
      Thread.sleep(3000)
      initialize(callback)
    }
  }

  def hasFreeSpace: Boolean = {
    for (i <- 0 to conf.ce_max_queue_size-1) {
      if (queue(i) == null)
        return true
    }
    return false
  }

  def enqueue (engineAlgo: EngineAlgorithm): Unit = {
    for (i <- 0 to conf.ce_max_queue_size-1) {
      if (queue(i) == null) {
        queue(i) = engineAlgo 
        engineAlgo.sendRequest(this)
        return
      }
    }
  }

  def finished (token: String): Unit = {
    for (i <- 0 to conf.ce_max_queue_size-1) {
      if (queue(i) != null && queue(i).token == token) {
        queue(i).computationComplete
        queue(i) = null
        return
      }
    }
  }

}
