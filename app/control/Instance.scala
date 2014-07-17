/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.concurrent._

import com.github.nscala_time.time.Imports._ 

import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits._

import shared.Neo4j
import shared.WebService
import control.http.InReports
import shared.Utils

/**
 * Enumerator used to set instance's status.
 */
object InstanceStatus extends Enumeration {
  val INITIALIZING, IDLE, COMPUTING, FAILED, DELETED = Value
}

/**
 * Object companion used to create Instance objects.
 */
object Instance {

  /**
   * Constructor that creates a Census Engine instance
   * from a GCE virtual machine.
   *
   * @param callback function to be executed when the Census Engine
   *                 service is ready to receive requests.
   * @return an Instance object.
   */
  def apply (callback: Instance=>Unit): Instance = {
    val instance = new Instance
    instance.initialize(callback)
    instance
  } 

  /**
   * Constructor that creates a Census Engine instance
   * from a preconfigured service.
   *
   * @param host name of the Census Engine server.
   * @param ip of the Census Engine server.
   * @param callback function to be executed when the Census Engine
   *                 service is ready to receive requests.
   * @return an Instance object.
   */
  def apply (host: String, ip: String, callback: Instance=>Unit): Instance = {
    val instance = new Instance
    instance.initializeWithHost(host, ip, callback)
    instance
  }

}

/**
 * Class with all the necessary functions to create and communicate with a
 * Census Engine server.
 */
class Instance extends WebService {

  /** Status of the instance. */
  var status: InstanceStatus.Value = InstanceStatus.INITIALIZING

  /** IP of the server. */
  var ip: String = ""

  /** Queue for the requests. */
  var queue: Array[Sender] = new Array(conf.ce_max_queue_size)

  /** 
   * Initializes the instance by creating a GCE
   * virtual machine.
   *
   * @param callback function to be executed when the Census Engine
   *                 instance is ready to receive requests.
   */
  private def initialize (callback: Instance=>Unit): Unit = {
    GCE.createInstance { (h, i, p) =>
      ip = i
      setHost(h, p)
      println(s"${DateTime.now} - INFO: Will wait for census engine service $host.")
      setCensusControlCommunication(callback)
    }
  }

  /**
   * Initializes the instance with a preconfigured
   * Census Engine server.
   *
   * @param h hostname of the Census Engine server.
   * @param i ip of the Census Engine server.
   * @param callback function to be executed when the Census Engine
   *                 service is ready to receive requests.
   */
  private def initializeWithHost (h: String, i: String, callback: Instance=>Unit): Unit = {
    ip = i
    setHost(h, conf.census_engine_port)
    setCensusControlCommunication(callback)
  }

  /**
   * Waits for the Census Engine service to be ready, then it registers this
   * Census Control service to the instance for bidirectional communication,
   * and finally registers this instance as a listener to the InReports module.
   *
   * @param callback function to be executed when the bidirectional communication is up.
   */
  private def setCensusControlCommunication (callback: Instance=>Unit): Unit = {
    ping map { response =>
      println(s"${DateTime.now} - INFO: Census engine service $host ready.")
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
      Thread.sleep(3000)
      setCensusControlCommunication(callback)
    }
  }

  /**
   * Called when the actual Census Engine server fails.
   */
  def failed: Unit = {
    status = InstanceStatus.FAILED
    println(s"${DateTime.now} - ERROR: Couldn't reach instance with host $host:$port.")
  }

  /**
   * Imports a graph for an algorithm to the actual Census Engine server.
   *
   * @param algorithm name to be used for the Census Engine format.
   * @param database from which the graph will be imported.
   * @param callback function to be executed when the graph import is successful.
   */
  def prepareForAlgorithm (algorithm: String, database: Neo4j, callback: ()=>Unit): Unit = {
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

  /**
   * Deletes this instance by unregistering it from the InReports module
   * and requesting the GCE api to delete the virtual machine.
   *
   * @param callback function to be executed when the GCE virtual machine 
   *                 deletion is done.
   */
  def delete (callback: ()=>Unit): Unit = {
    InReports.unregister(this)
    GCE.deleteInstance(host, { () => 
      status = InstanceStatus.DELETED
      callback()
    })
  }

  /**
   * Checks if the queue has at least one space for a request.
   *
   * @return 'true' if it has space.
   *         'false' if it doesn't.
   */
  def hasFreeSpace: Boolean = {
    for (request <- queue) {
      if (request == null) return true
    }
    return false
  }

  /**
   * Adds an EngineRequest with the Sender interface to the 
   * queue and sends it immediately.
   *
   * @param engineRequest that will be added and sent.
   */
  def send (engineRequest: Sender): Unit = {
    for (i <- 0 to (queue.length-1)) {
      if (queue(i) == null) {
        queue(i) = engineRequest 
        engineRequest.send(this)
        return
      }
    }
  }

  /**
   * Invoked by the InReports module when a report arrives
   * from the Census Engine server. Dequeues the request
   * and calls the complete method of the EngineRequest with
   * the Sender interface.
   * 
   * @param token of the request that is being reported.
   */
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

  /**
   * Invoked by the InReports module when an error arrives
   * from the Census Engine server. 
   * 
   * @param token of the request that is being reported.
   * @param error description.
   * @param on operation.
   */
  def error (token: String, error: String, on: String) {
    println(s"${DateTime.now} - ERROR: $error on $on for token: $token.")
  }

}
