/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import requests.ComputationRequest
import controllers.N4j

trait EngineAlgorithm {
  
  var status: String = "pending"

  var token: String = null

  var database: N4j = null

  var requester: ComputationRequest = null

  var requestData: String = null
  
  def enqueue: Unit

}
