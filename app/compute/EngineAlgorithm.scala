/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import requests.ComputationRequest

trait EngineAlgorithm {
  
  var status: String = "pending"

  var requester: ComputationRequest = null

  def enqueue: Unit

}
