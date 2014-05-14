/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import scala.concurrent._

import com.github.nscala_time.time.Imports._ 

import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits._

import controllers.N4j
import controllers.WebService
import requests.ComputationRequest
import requests.Utils
import compute.EngineRequest

object InstanceStatus extends Enumeration {
  val INITIALIZING, IDLE, COMPUTING, FAILED = Value
}

object Instance {

  def apply (callback: Instance=>Unit): Instance = {
    val instance = new Instance
    instance.initialize(callback)
    instance
  } 

  def apply (host: String, callback: Instance=>Unit): Instance = {
    val instance = new Instance
    instance.initializeWithHost(host, callback)
    instance
  }

}

class Instance extends WebService {

  var status: InstanceStatus.Value = InstanceStatus.INITIALIZING

  var activeRequest: ComputationRequest = null

  /** Queue for the requests. */
  var queue: Array[EngineRequest] = new Array[EngineRequest](conf.ce_max_queue_size)

  private def initialize (callback: Instance=>Unit): Unit = {
    GCE.createInstance { (h, p) =>
      setHost(h, p)
      setCensusControlCommunication(callback)
    }
  }

  private def initializeWithHost (host: String, callback: Instance=>Unit): Unit = {
    setHost(host, conf.census_engine_port)
    setCensusControlCommunication(callback)
  }

  private def setCensusControlCommunication (callback: Instance=>Unit): Unit = {
    ping map { response =>
      post("/control", "{"
        +s""" "host": "${conf.census_control_host}", """
        +s""" "port": ${conf.census_control_port} """
        + "}"
      ) map { res => 
        status = InstanceStatus.IDLE
        callback(this) 
      }
    } recover { case _ => 
      println(s"${DateTime.now} - INFO: Service $host still not ready, will wait 3 seconds.")
      Thread.sleep(3000)
      setCensusControlCommunication(callback)
    }
  }

  private def instanceFailed: Unit = {
    status = InstanceStatus.FAILED
    println(s"${DateTime.now} - ERROR: Couldn't reach instance with host $host:$port.")
  }

  def prepareForRequest (requester: ComputationRequest, callback: ()=>Unit): Unit = {
    activeRequest = requester
    // Import graph.
    post("/graph", "{"
      +s""" "token": "${Utils.genUUID}", """
      +s""" "algorithm": "${requester.algorithm.name}", """
      +s""" "tag": "${requester.tag}", """
      +s""" "host": "${requester.database.host}", """
      +s""" "port": ${requester.database.port}, """
      +s""" "user": "${requester.database.user}", """
      +s""" "password": "${requester.database.password}" """
      + "}"
    ) map { response => 
      val status = (response.json \ "status").as[String] 
      if (status == "acknowledged")
        callback()
      else
        println(s"${DateTime.now} - ERROR: Census Engine response status:$status on graph import, please check for bugs.")
    } recover {
      case _ => instanceFailed
    }
  }

  def delete (callback: ()=>Unit): Unit = {
    GCE.deleteInstance(host, callback)
  }

  def hasFreeSpace: Boolean = {
    for (request <- queue) {
      if (request == null) return true
    }
    return false
  }

  def send (engineRequest: EngineRequest): Unit = {
    for (i <- 0 to (queue.length-1)) {
      if (queue(i) == null) {
        queue(i) = engineRequest 
        engineRequest.send(this)
        return
      }
    }
  }

  def finished (token: String): Unit = {
    for (i <- 0 to (queue.length-1)) {
      if (queue(i) != null && queue(i).token == token) {
        queue(i).computationComplete
        queue(i) = null
        checkIfIdle
        return
      }
    }
  }

  private def checkIfIdle: Unit = {
    for (request <- queue) {
      if (request != null) return
    }
    status = InstanceStatus.IDLE
  }

}
