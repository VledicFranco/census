/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

/**
 * Interface for EngineRequests that are initiated by the
 * ComputeRequest.
 *
 * NOTE: The Receiver and Sender interfaces are designed so that
 * MultiNodeRequests and SingleGraphRequests are implementable.
 */
trait Receiver {

  /**
   * Called by a ComputeRequest to initiate an 
   * EngineRequest.
   */
  def receive: Unit

}

