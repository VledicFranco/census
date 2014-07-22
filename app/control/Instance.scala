/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.concurrent._

import shared.WebService
import shared.Log
import http.InReports

/**
 * Object companion used to create Instance objects.
 */
object Instance {

  /**
   * Constructor that creates a Census Engine instance
   * from a GCE virtual machine or in the localhost.
   *
   * @param callback function to be executed when the Census Engine
   *                 service is ready to receive requests.
   * @return an Instance object.
   */
  def apply (local: Boolean, onReport: (Instance, String)=>Unit, onError: (Instance, String, String)=>Unit, callback: Instance=>Unit): Unit = {
    if (local)
      (new Instance("127.0.0.1", "localhost", conf.census_port, onReport, onError)).setCommunication(callback)
    else
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
  val user = null
  val password = null

  /**
   * Waits for the Census Engine service to be ready, then it registers this
   * Census Control service to the instance for bidirectional communication,
   * and finally registers this instance as a listener to the InReports module.
   *
   * @param callback function to be executed when the bidirectional communication is up.
   */
  private def setCommunication (callback: Instance=>Unit): Unit = {
    Log.info(s"Waiting for Census Engine service: $host")
    ping { success =>
      if (!success) {
        Thread.sleep(1000)
        return setCommunication(callback)
      }
      post("/reports", Json.obj(
        "host" -> conf.census_control_host,
        "port" -> conf.census_port
      ), { (error, response) =>
        if (error) return commError("POST: /reports request.")
        InReports.register(this)
        callback(this) 
      })
    }
  }

  /**
   * Called when the Census Engine server is unreachable.
   */
  private def commError (token: String) = 
    onError(this, token, "communication-lost")

  /**
   * Called by the InReports module when a report arrives
   * from the Census Engine server. 
   * 
   * @param token of the request that is being reported.
   */
  def report (token: String) = 
    onReport(this, token)

  /**
   * Called by the InReports module when an error arrives
   * from the Census Engine server. 
   * 
   * @param token of the request that is being reported.
   * @param error description.
   */
  def error (token: String, error: String) = 
    onError(this, token, error)

  /**
   * Sends a graph import request.
   *
   * @param request with token and payload.
   */
  def import (request: EngineRequest) =
    post("/engine/import", request.payload, { (error, response) =>
      if (error) commError(request.token)
    })

  /**
   * Sends an algorithm compute request.
   *
   * @param request with token and payload.
   */
  def compute (request: EngineRequest) =
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
