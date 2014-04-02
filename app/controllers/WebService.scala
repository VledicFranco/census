/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers

import scala.concurrent.Future

import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * An extern web service which can be called 
 * through http requests.
 */
trait WebService {
  
  /** Port where the web service is listening. */
  var port: Int = 80

  /** Host name where the web service is located. */
  var host: String = "localhost"

  /** User for authenticating requests. */
  var user: String = "root"

  /** Password for authenticating requests. */
  var password: String = "root"

  /** Timeout used for reachability testing. */
  val timeoutLength: Int = 2000

  /**
   * Hostname and port setter.
   *
   * @param h hostname.
   * @param p port.
   */
  def setHost (h: String, p: Int): Unit = {
    host = h
    port = p
  }

  /**
   * Authentication information setter.
   *
   * @param u username.
   * @param p password.
   */
  def setAuth (u: String, p: String): Unit = {
    user = u
    password = p
  }

  /**
   * Uses the Play Framework WS api to post something
   * to the web service.
   *
   * @param path to post to.
   * @param data to be posted, normally a json string.
   * @return a future that handles the web service response.
   */
  def post (path: String, data: String): Future[Response] = {
    WS.url(s"http://$host:$port$path").post(data)
  }

  /**
   * Makes a request to the web service with HEAD method
   * and a timeout to test reachability.
   *
   * @return a future that handles the web service response.
   */
  def ping: Future[Response] = {
    WS.url(s"http://$host:$port").withTimeout(2000).head()
  }

}
