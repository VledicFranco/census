/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import requests.Utils
import requests.ComputationRequest
import controllers.N4j
import instances.Instance

trait EngineAlgorithm {
  
  var status: String = "pending"

  var token: String = Utils.genUUID

  val creationTime: Long = System.currentTimeMillis

  var database: N4j = null

  var requester: ComputationRequest = null

  var requestData: String = null
  
  def enqueue: Unit

  def sendRequest (instance: Instance): Unit

  def computationComplete: Unit

}
