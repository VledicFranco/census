/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package utils 

/**
 * Utility functions module.
 */
object Utils {

  /**
   * Generates a Universal Unique Identifier.
   *
   * @return the uuid string.
   */
  def genUUID: String = {
    java.util.UUID.randomUUID.toString
  }

}
