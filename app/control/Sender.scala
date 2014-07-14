/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import instances.Instance

/**
 * Interface for EngineRequests, used to send the actual
 * HTTP request to a CensusEngine instance.
 *
 * NOTE: The Receiver and Sender interfaces are designed so that
 * MultiNodeRequests and SingleGraphRequests are implementable.
 */
trait Sender {
  
  /** A UUID string used to identify the request. */
  /** NOTE: This attribute is concreted by the EngineRequest trait. */
  val token: String

  /** 
   * Invoked by the instance when the request enters it's queue.
   * Sends the acutal HTTP request.
   * 
   * @param instance which will receive the HTTP request.
   */
  def send (instance: Instance): Unit

  /**
   * Invoked by the instance when the request is complete.
   */
  def complete: Unit

}
