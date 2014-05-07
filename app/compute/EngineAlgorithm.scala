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

trait EngineAlgorithm {
  
  val name: String

  val requester: ComputationRequest

  var status: String = "pending"

  val token: String = Utils.genUUID

  val creationTime: Long = System.currentTimeMillis

  def enqueue: Unit
  
  def sendComputationRequest (instance: Instance): Unit

  def computationComplete: Unit

  def sendRequest (instance: Instance): Unit = {
    if (instance.activeDatabase != requester.database
      || instance.activeAlgorithm != name) {
      instance.activeDatabase = requester.database
      instance.activeAlgorithm = name
      sendImportGraphRequest(instance, sendComputationRequest)
    } else {
      sendComputationRequest(instance)
    }
  }

  def sendImportGraphRequest (instance: Instance, callback: Instance=>Unit): Unit = {    
    instance.post("/graph", "{"
      +s""" "token": "${requester.database.host}", """
      +s""" "algorithm": "$name", """
      +s""" "tag": "${requester.tag}", """
      +s""" "host": "${requester.database.host}", """
      +s""" "port": ${requester.database.port}, """
      +s""" "user": "${requester.database.user}", """
      +s""" "password": "${requester.database.password}" """
      + "}"
    ) map {
      res => {
        validateGraphImportRequest(res.json)
        callback(instance)
      }
    } recover {
      // Handle instance failure here.
      case _ => {
        println(s"${DateTime.now} - ERROR: Couldn't reach instance with host ${instance.host}:${instance.port}.")
      }
    }
  }

  def validateGraphImportRequest (json: JsValue): Unit = {
    (json \ "status").asOpt[String] match {
      case Some(resStatus) =>
        if (resStatus != "acknowledged") {
          println(s"${DateTime.now} - ERROR: Census Engine response status:$resStatus please check for bugs.")
        }
      case None => println(s"${DateTime.now} - ERROR: Invalid Census Engine response, please check for bugs.")
    }
  }

}
