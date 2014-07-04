/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import com.github.nscala_time.time.Imports._ 

import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent._
import scala.collection.mutable.Queue

import compute.Sender
import controllers.Neo4j
import controllers.requests.ComputationRequest

/**
 * Companion object to create Orchestrators.
 */
object Orchestrator {
  
  /**
   * Constructor that initializes an orchestrator with a certain amount of
   * instances, an algorithm to be imported, and a database to be used.
   *
   * @param size or amount of instances to be created and orchestrated.
   * @param algorithm to format the Census Engine services.
   * @param database to be used to import the graph to the Census Engine services.
   * @param callback function to be executed after all the Census Engine services are ready.
   */
  def apply (size: Int, algorithm: String, database: Neo4j, callback: Orchestrator=>Unit): Orchestrator = {
    val orchestrator = new Orchestrator(size, algorithm, database)
    orchestrator.initialize(callback)
    orchestrator
  }

}

/**
 * Class that creates and orchestrates a certain amount of Census Engine instances.
 *
 * @param size or amount of instances to be created and orchestrated.
 * @param algorithm to format the Census Engine services.
 * @param database to be used to import the graph to the Census Engine services.
 */
class Orchestrator (val size: Int, val algorithm: String, val database: Neo4j) {

  /** Used to know if the requests are being processed. */
  private var isRunning: Boolean = false

  /** Array that holds all the orchestrated instances. */
  private val pool: Array[Instance] = new Array[Instance](size)

  /** Queue for the requests. */
  private val queue: Queue[Sender] = Queue()

  /**
   * Initializes the orchestrator, starts the Census Engine instance
   * creation.
   *
   * @param callback function to be executed after all the Census Engine services are ready.
   */
  private def initialize (callback: Orchestrator=>Unit): Unit = {
    for (i <- 0 to (pool.length-1)) {
      pool(i) = Instance({ instance =>
        instance.prepareForAlgorithm(algorithm, database, { () =>
          if (poolIsReady) callback(this)
        })
      })  
    }
  }

  /**
   * Checks if all the instances in the pool have status 'IDLE'.
   *
   * @return 'true' if all instances have status 'IDLE'.
   *         'false' if at least one instance has a different status.
   */
  private def poolIsReady: Boolean = {
    for (instance <- pool) {
      if (instance.status != InstanceStatus.IDLE) return false
    }
    return true
  }

  /**
   * Checks if all the instances in the pool have status 'DELETED'.
   *
   * @return 'true' if all instances have status 'DELETED'.
   *         'false' if at least one instance has a different status.
   */
  private def poolIsDeleted: Boolean = {
    for (instance <- pool) {
      if (instance.status != InstanceStatus.DELETED) return false
    }
    return true
  }

  /**
   * Deletes all the instances in the pool.
   *
   * @param callback function to be executed when all the instances
   *                 are deleted.
   */
  def delete (callback: ()=>Unit): Unit = {
    for (instance <- pool) {
      instance.delete { () =>
        if (poolIsDeleted) callback()
      }
    }
  }

  /**
   * Adds a request to the queue and starts it if the queue is idle.
   *
   * @param request to be enqueued.
   */
  def enqueue (request: Sender): Unit = {
    queue += request
    if (!isRunning) {
      isRunning = true
      future { continue }
    }
  }

  /**
   * Executes the next request in the queue, enqueues it
   * to the first free instance it finds, continues untill
   * there are no more free instances. The queue is restarted 
   * when a request in an instance is finished.
   */
  def continue: Unit = {
    if (queue.isEmpty) {
      isRunning = false
      return
    }
    var foundFreeInstance = false
    for (instance <- pool) {
      if (instance.hasFreeSpace) {
        foundFreeInstance = true
        instance.send(queue.dequeue)
      }
    }
    if (foundFreeInstance) continue
  }

}
