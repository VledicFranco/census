/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute.library

import com.github.nscala_time.time.Imports._ 

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import compute.SingleNodeRequest
import compute.MultiNodeRequest
import control.requests.ComputeRequest
import instances.Instance

/**
 * SingleNodeRequest implementation for the Freeman's Closeness.
 */
class BrandesBetweennessSN (val source: String, val parent: MultiNodeRequest, val requester: ComputeRequest) extends SingleNodeRequest {

  /** 
   * Invoked by the instance when the request enters it's queue.
   * Sends the acutal HTTP request.
   * 
   * @param instance which will receive the HTTP request.
   */
  def send (instance: Instance): Unit = {
    instance.post("/compute", "{"
      +s""" "token": "$token", """
      + """ "algorithm": "BrandesBetweenness", """
      +s""" "creationTime": $creationTime, """
      +s""" "vars": ["$source"] """
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
