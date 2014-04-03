/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

/** 
 * An in simple request that executes synchronous.
 */
trait SimpleRequest extends Request {

  def execute: Unit

}
