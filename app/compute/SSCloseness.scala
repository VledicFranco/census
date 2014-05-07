/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import com.github.nscala_time.time.Imports._ 

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

  var parentEngineAlgorithm: EngineAlgorithm = null

  def enqueue: Unit = {
    Orchestrator.enqueue(this)
  }

  def sendRequest (instance: Instance): Unit = {
    instance.post("/compute", "{"
      +s""" "token": "$token", """
      + """ "algorithm": "SSCloseness", """
      +s""" "creationTime": $creationTime, """
      +s""" "vars": { "source": "$source" } """
      + "}"
    ) map {
      res => validateJson(res.json)
    } recover {
      // Handle instance failure here.
      case _ => {
        println(s"${DateTime.now} - ERROR: Couldn't reach instance with host ${instance.host}:${instance.port}.")
      }
    }
  }

  def validateJson (json: JsValue): Unit = {
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

  def sendImportGraphRequest (instance: Instance): Unit = {}

  def computationComplete: Unit = {
    status = "finished"
    // Collect to the parent request.
  }

}
