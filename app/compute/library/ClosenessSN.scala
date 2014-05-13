/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute.library

import com.github.nscala_time.time.Imports._ 

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import requests.ComputationRequest
import controllers.N4j
import instances.Orchestrator
import instances.Instance

class ClosenessSN (val source: String, val requester: ComputationRequest) extends EngineAlgorithm {

  val name = "SSCloseness"

  var parentEngineAlgorithm: EngineAlgorithm = null

  def enqueue: Unit = {
    Orchestrator.enqueue(this)
  }

  def sendComputationRequest (instance: Instance): Unit = {
    instance.post("/compute", "{"
      +s""" "token": "$token", """
      + """ "algorithm": "SSCloseness", """
      +s""" "creationTime": $creationTime, """
      +s""" "vars": { "source": "$source" } """
      + "}"
    ) map {
      res => validateRequest(res.json)
    } recover {
      // Handle instance failure here.
      case _ => {
        println(s"${DateTime.now} - ERROR: Couldn't reach instance with host ${instance.host}:${instance.port}.")
      }
    }
  }

  def validateRequest (json: JsValue): Unit = {
    (json \ "status").asOpt[String] match {
      case Some(resStatus) =>
        if (resStatus == "acknowledged") {
          status = "computing"
        } else {
          println(s"${DateTime.now} - ERROR: Census Engine response status:$resStatus please check for bugs.")
        }
      case None => println(s"${DateTime.now} - ERROR: Invalid Census Engine response, please check for bugs.")
    }
  }

  def computationComplete: Unit = {
    status = "finished"
    // Collect to the parent request.
  }

}
