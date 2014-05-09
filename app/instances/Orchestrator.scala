/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import com.github.nscala_time.time.Imports._ 

import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent._
import scala.collection.mutable.Queue

import compute.EngineAlgorithm

/**
 * Module that enqueues requests, processed as first
 * come first served.
 */
object Orchestrator {

  /** Used to know if the requests are being processed. */
  var isRunning: Boolean = false

  /** Queue for the requests. */
  var queue: Queue[EngineAlgorithm] = Queue()

  var pool: Array[Instance] = new Array[Instance](conf.max_instances) 

  /**
   * Adds a request to the queue and starts the
   * processing if the queue is idle.
   *
   * @param req
   */
  def enqueue (req: EngineAlgorithm): Unit = {
    // Add request to the queue.
    queue.synchronized {
      queue += req
    }

    if (isRunning) return
    isRunning = true

    if (poolIsEmpty) {
      createInstance(next)
    } else {
      future { next }
    }
  }

  def finished (host: String, token: String): Unit = {
    val instance = getInstance(host)
    instance.finished(token)
    next()
  }

  private def createInstance (callback: ()=>Unit): Unit = {
//    val ip = "127.0.0.1"
//    val port = 9000
//    val instance = new Instance(ip, port)
//    // Register Census Control HTTP hook.
//    instance.post("/control", "{"
//      +s""" "host": "${conf.census_control_host}", """
//      +s""" "port": ${conf.census_control_port} """
//      + "}"
//    ) map { res => 
//      addInstanceToPool(instance) 
//      callback()
//    } recover {
//      case _ => println(s"${DateTime.now} - ERROR: Couldn't reach the new instance with host $ip:$port.")
//    }
  }

  private def addInstanceToPool (instance: Instance): Unit = {
    for (i <- 0 to conf.max_instances-1) {
      if (pool(i) == null) {
        pool(i) = instance
        return
      }
    }
  }

  private def getInstance (host:String): Instance = {
    for (i <- 0 to conf.max_instances-1) {
      if (pool(i) != null && pool(i).host == host) 
        return pool(i)
    }
    return null
  }

  private def poolIsEmpty: Boolean = {
    for (i <- 0 to conf.max_instances-1) {
      if (pool(i) != null) return false 
    }
    return true
  }

  /**
   * Executes the next request in the queue, passes 
   * himself as a callback of the request so that 
   * the queue continues after the request is done.
   */
  private def next (): Unit = {
    // Terminate if there is no next request.
    if (queue.isEmpty) {
      isRunning = false
      return
    }
    
    var foundFreeInstance = false

    for (instance: Instance <- pool) {
      if (instance.hasFreeSpace) {
        foundFreeInstance = true
        instance.enqueue(queue.dequeue)
      }
    }

    // If instances are full and still less than the allowed max
    // create a new instance and start using it.
    
    // Continue untill instances are full.
    // The queue is restarted when the requests in the instances
    // are finished.
    if (foundFreeInstance) next
  }

}
