/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

trait Receiver {
  
  val token: String

  def receive: Unit

}

