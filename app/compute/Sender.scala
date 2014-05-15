/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import instances.Instance

trait Sender {

  val token: String
  
  def send (instance: Instance): Unit

  def complete: Unit

}
