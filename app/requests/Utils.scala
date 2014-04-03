/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

object Utils {

  def genUUID: String = {
    java.util.UUID.randomUUID.toString
  }

}
