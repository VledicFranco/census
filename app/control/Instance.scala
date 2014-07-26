/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.concurrent._

import play.api.libs.json._

import shared.WebService
import shared.Log
import http.InReports

/**
 * Object companion used to create Instance objects.
 */
object Instance {

  def apply (ip: String, host: String, port: Int, onReport: (Instance, String)=>Unit, onError: (Instance, String, String)=>Unit, callback: Instance=>Unit): Unit = {
    (new Instance(ip, host, port, onReport, onError)).setCommunication(callback)
  } 

  def apply (onReport: (Instance, String)=>Unit, onError: (Instance, String, String)=>Unit, callback: Instance=>Unit): Unit = {
    GCE.createInstance { (ip, host) => 
      (new Instance (ip, host, conf.census_port, onReport, onError)).setCommunication(callback)
    }
  } 

}

/**
 * Class with all the necessary functions to create and communicate with a
 * Census Engine server.
 */
class Instance (
  val ip: String, 
  val host: String, 
  val port: Int, 
  onReport: (Instance, String)=>Unit, 
  onError: (Instance, String, String)=>Unit) 
extends WebService {

  /** WebService trait attributes to be defined. */
  val user: String = null
  val password: String = null

  /**
   * Waits for the Census Engine service to be ready, then it registers this
   * Census Control service to the instance for bidirectional communication,
   * and finally registers this instance as a listener to the InReports module.
   *
   * @param callback function to be executed when the bidirectional communication is up.
   */
  private def setCommunication (callback: Instance=>Unit): Unit = {
    Log.info(s"STARTING: $host")
    ping { success =>
      if (!success) {
        Thread.sleep(1000)
        setCommunication(callback)
      } else {
        val reportsHost = if (ip == "127.0.0.1") ip else conf.census_control_host
        val json = Json.obj(
          "host" -> reportsHost,
          "port" -> conf.census_port
        )
        post("/reports", json, { (error, response) =>
          if (error)
            commError("POST: /reports request.")
          else {
            Log.info(s"READY: $host")
            InReports.register(this)
            callback(this) 
          }
        })
      }
    }
  }

  /**
   * Called when the Census Engine server is unreachable.
   */
  private def commError (token: String): Unit = 
    onError(this, token, "communication-lost")

  /**
   * Called by the InReports module when a report arrives
   * from the Census Engine server. 
   * 
   * @param token of the request that is being reported.
   */
  def report (token: String): Unit = 
    onReport(this, token)

  /**
   * Called by the InReports module when an error arrives
   * from the Census Engine server. 
   * 
   * @param token of the request that is being reported.
   * @param error description.
   */
  def error (token: String, error: String): Unit = 
    onError(this, token, error)

  /**
   * Sends a graph import request.
   *
   * @param request with token and payload.
   */
  def importGraph (request: EngineRequest): Unit =
    post("/engine/import", request.payload, { (error, response) =>
      if (error) commError(request.token)
    })

  /**
   * Sends an algorithm compute request.
   *
   * @param request with token and payload.
   */
  def compute (request: EngineRequest): Unit =
    post("/engine/compute", request.payload, { (error, response) =>
      if (error) commError(request.token)
    })

  /**
   * Deletes this instance by unregistering it from the InReports module
   * and requesting the GCE api to delete the virtual machine.
   *
   * @param callback function to be executed when the GCE virtual machine 
   *                 deletion is done.
   */
  def delete (callback: Unit=>Unit): Unit = {
    InReports.unregister(this)
    if (ip == "127.0.0.1") 
      callback()
    else
      GCE.deleteInstance(host, callback)
  }

}
