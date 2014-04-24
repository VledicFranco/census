/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import requests.ComputationRequest

object Closeness {

  def apply (r: ComputationRequest): Closeness = {
    val algo = new Closeness(r)
    algo
  }

}

class Closeness (val r: ComputationRequest) extends EngineAlgorithm {

  this.requester = r
  
  def enqueue: Unit = {
    println("HOLA :)")
  }

}
