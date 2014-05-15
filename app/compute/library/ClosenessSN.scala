/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute.library

import com.github.nscala_time.time.Imports._ 

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import compute.SingleNodeRequest
import compute.MultiNodeRequest
import requests.ComputationRequest
import instances.Instance

class ClosenessSN (val source: String, val parent: MultiNodeRequest, val requester: ComputationRequest) extends SingleNodeRequest {

  def send (instance: Instance): Unit = {
    instance.post("/compute", "{"
      +s""" "token": "$token", """
      + """ "algorithm": "SSCloseness", """
      +s""" "creationTime": $creationTime, """
      +s""" "vars": { "source": "$source" } """
      + "}"
    ) map {
      response =>
        val status = (response.json \ "status").as[String] 
        if (status != "acknowledged") {
          println(s"${DateTime.now} - ERROR: Census Engine response status:$status please check for bugs.")
          println(response.json)
        }
    } recover {
      case _ => instance.failed
    }
  }

}
