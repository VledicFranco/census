/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

trait InReportsListener {

  def report (host: String, token: String): Unit

}
