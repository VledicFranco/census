/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

trait Sender {
  
  def send (instance: Instance): Unit

  def complete: Unit

}
