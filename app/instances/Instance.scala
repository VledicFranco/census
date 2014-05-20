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
import controllers.InReports
import requests.Utils
import compute.Sender

object InstanceStatus extends Enumeration {
  val INITIALIZING, IDLE, COMPUTING, FAILED = Value
}

object Instance {

  def apply (callback: Instance=>Unit): Instance = {
    val instance = new Instance
    instance.initialize(callback)
    instance
  } 

  def apply (host: String, ip: String, callback: Instance=>Unit): Instance = {
    val instance = new Instance
    instance.initializeWithHost(host, ip, callback)
    instance
  }

}

class Instance extends WebService {

  var status: InstanceStatus.Value = InstanceStatus.INITIALIZING

  var ip: String = ""

  /** Queue for the requests. */
  var queue: Array[Sender] = new Array(conf.ce_max_queue_size)

  private def initialize (callback: Instance=>Unit): Unit = {
    GCE.createInstance { (h, i, p) =>
      ip = i
      setHost(h, p)
      setCensusControlCommunication(callback)
    }
  }

  private def initializeWithHost (h: String, i: String, callback: Instance=>Unit): Unit = {
    ip = i
    setHost(h, conf.census_engine_port)
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
        InReports.register(this)
        callback(this) 
      }
    } recover { case _ => 
      println(s"${DateTime.now} - INFO: Service $host still not ready, will wait 3 seconds.")
      Thread.sleep(3000)
      setCensusControlCommunication(callback)
    }
  }

  def failed: Unit = {
    status = InstanceStatus.FAILED
    println(s"${DateTime.now} - ERROR: Couldn't reach instance with host $host:$port.")
  }

  def prepareForAlgorithm (algorithm: String, database: N4j, callback: ()=>Unit): Unit = {
    // Import graph.
    post("/graph", "{"
      +s""" "token": "${Utils.genUUID}", """
      +s""" "algorithm": "$algorithm", """
      +s""" "tag": "${database.tag}", """
      +s""" "host": "${database.host}", """
      +s""" "port": ${database.port}, """
      +s""" "user": "${database.user}", """
      +s""" "password": "${database.password}" """
      + "}"
    ) map { response => 
      val status = (response.json \ "status").as[String] 
      if (status == "acknowledged")
        callback()
      else
        println(s"${DateTime.now} - ERROR: Census Engine response status:$status on graph import, please check for bugs.")
    } recover {
      case _ => failed
    }
  }

  def delete (callback: ()=>Unit): Unit = {
    InReports.unregister(this)
    GCE.deleteInstance(host, callback)
  }

  def hasFreeSpace: Boolean = {
    for (request <- queue) {
      if (request == null) return true
    }
    return false
  }

  def send (engineRequest: Sender): Unit = {
    for (i <- 0 to (queue.length-1)) {
      if (queue(i) == null) {
        queue(i) = engineRequest 
        engineRequest.send(this)
        return
      }
    }
  }

  def report (token: String): Unit = {
    for (i <- 0 to (queue.length-1)) {
      if (queue(i) != null && queue(i).token == token) {
        queue(i).complete
        queue(i) = null
        // If there are pending requests just return
        // else set to IDLE.
        for (request <- queue) {
          if (request != null) return
        }
        status = InstanceStatus.IDLE
        return
      }
    }
  }

  def error (token: String, error: String, on: String) {
    println(s"${DateTime.now} - ERROR: $error on $on for token: $token.")
  }

}
