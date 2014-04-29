/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import requests.ComputationRequest
import controllers.N4j
import instances.Orchestrator
import instances.Instance

object SSCloseness {

  def apply (source: String, r: ComputationRequest): SSCloseness = {
    val algo = new SSCloseness(source, r)
    algo
  }

}

class SSCloseness (val source: String, val r: ComputationRequest) extends EngineAlgorithm {

  requester = r

  var parentToken: String = null

  def enqueue: Unit = {
    println(source)
  }

  def sendRequest (instance: Instance): Unit = {
    instance.post("/compute", "{"
      +s""" "token": "$token", """
      + """ "algorithm": "SSCloseness", """
      +s""" "creationTime": "$creationTime", """
      +s""" "vars": { "source": $source } """
      + "}"
    ) map {
      res => validateJson(res.json)
    } recover {
      // Handle instance failure here.
      case _ => println("Unreachable instance.")
    }
  }

  def validateJson (json: JsValue): Unit = {
    (json \ "status").asOpt[String] match {
      case Some(resStatus) =>
        if (resStatus == "acknowledged") {
          status = "computing"
        }
      case None => println("Invalid Census Engine response, please check for bugs.")
    }
  }

}
