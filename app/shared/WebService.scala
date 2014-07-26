/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package shared

import scala.concurrent.Future

import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits._

/** An external web service which can be called through http requests. */
trait WebService {

  /** Port where the web service is listening. */
  val port: Int

  /** Host name where the web service is located. */
  val host: String

  /** User for authenticating requests. */
  val user: String

  /** Password for authenticating requests. */
  val password: String

  /** Timeout used for reachability testing. */
  private val timeoutLength: Int = 2000

  /** Uses the Play Framework WS api to post something to the web service.
    *
    * @param path to post to.
    * @param data to be posted.
    * @param callback (Boolean, Response)=>Unit callback with 
    *                 first parameter 'true' if there was an error.
    */
  def post (path: String, data: JsValue, callback: (Boolean, Response)=>Unit): Unit = {
    var requestHolder = WS.url(s"http://$host:$port$path").withHeaders("Content-Type" -> "application/json")
    if (user != null && password != null) {
      requestHolder = requestHolder.withAuth(user, password, com.ning.http.client.Realm.AuthScheme.BASIC)
    }
    requestHolder.post(data) map { response =>
      if (response.status == 200)
        callback(false, response)
      else 
        callback(true, response)
    } 
  }

  /** Makes a request to the web service with HEAD method and a timeout to test reachability.
    *
    * @param callback Boolean=>Unit callback with parameter 
    *                 'true' if ping was successful.
    */
  def ping (callback: Boolean=>Unit): Unit = {
    var requestHolder = WS.url(s"http://$host:$port").withTimeout(2000)
    if (user != null && password != null) {
      requestHolder = requestHolder.withAuth(user, password, com.ning.http.client.Realm.AuthScheme.BASIC)
    }
    requestHolder.head() map { response => 
      if (response.status == 200)
        callback(true) 
      else
        callback(false) 
    } 
  }

}
