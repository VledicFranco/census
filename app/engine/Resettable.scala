/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

/**
 * Interface to reset parameters
 * of an object.
 */
trait Resettable {
  
  /** 
   * Used to reset parameters of an object.
   *
   * @param variables that may be used.
   */
  def reset (variables: Array[String]): Unit

}
