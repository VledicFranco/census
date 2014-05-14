/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import requests.Utils
import requests.ComputationRequest

trait EngineRequest {
  
  val algorithmName: String

  val requester: ComputationRequest

  var completed: Boolean = false

  val token: String = Utils.genUUID

  val creationTime: Long = System.currentTimeMillis

}
