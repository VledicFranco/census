/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import requests.ComputationRequest
import controllers.N4j
import instances.Orchestrator

object SSCloseness {

  def apply (source: String, r: ComputationRequest): SSCloseness = {
    val algo = new SSCloseness(source, r)
    algo
  }

}

class SSCloseness (val source: String, val r: ComputationRequest) extends EngineAlgorithm {

  this.requester = r

  def enqueue: Unit = {
    println(source)
  }

}
