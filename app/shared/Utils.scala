/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package shared

import com.github.nscala_time.time.Imports._ 

/** Utility functions module. */
object Utils {

  /** Generates a Universal Unique Identifier.
    *
    * @return the uuid string.
    */
  def genUUID: String = {
    java.util.UUID.randomUUID.toString
  }

}

/** Object to centralize logging. */
object Log {

  /** Prints a message with the date prepended. */
  def info (msg: String) = 
    println(s"${DateTime.now} - INFO: $msg")

  /** Prints a message error with the date prepended. */
  def error (msg: String) = 
    println(s"${DateTime.now} - ERROR: $msg")

}
