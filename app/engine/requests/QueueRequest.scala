/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers.requests

/**
 * An in request to be added to the RequestsQueue,
 * can be asynchronous.
 */
trait QueueRequest extends Request {

  /** Used to store the callback of the execute method. */
  var finish: ()=>Unit = null

  /**
   * Execute with a callback to support async methods
   * without breaking the queue line.
   *
   * @param callback to be executed when the execution ends.
   */
  def init (callback: ()=>Unit): Unit = {
    finish = callback
    execute
  }

}
