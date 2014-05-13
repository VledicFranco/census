/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import com.github.nscala_time.time.Imports._ 

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import requests.Utils
import requests.ComputationRequest
import controllers.N4j
import instances.Instance

trait EngineRequest {
  
  val name: String

  val requester: ComputationRequest

  var status: String = "pending"

  val token: String = Utils.genUUID

  val creationTime: Long = System.currentTimeMillis

  def start: Unit

  def complete: Unit

}
