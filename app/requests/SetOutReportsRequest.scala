/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import play.api.libs.json._

/** Verifies and encapsulates all the parameters needed to register an http hook for [[http.OutReports]] to use.
  *
  * A json example with all possible parameters:
  {{{
{
  "token": "asdw-12d24-awdqsr1-qwed2",
  "host": "census-control",
  "port": 9000
}
  }}}
  * And here the public data structure attributes that has the request:
  {{{
class ControlComputeRequest {
  val token: String
  val host: String
  val port: Int
}
  }}}
  *
  * @constructor creates a data structure with all the request's parameters.
  * @param json of the request.
  */
class SetOutReportsRequest (json: JsValue) extends Request {

  /** The request uniquer identifier. */
  val token: String = null

  /** The http hook hostname. */
  val host: String =
    (json \ "host").asOpt[String] match {
      case None => errors += "'host' field missing."; ""
      case Some(data) => data replaceAll ("http://", "")
    }
 
  /** The http hook port. */
  var port: Int =
    (json \ "port").asOpt[Int] match {
      case None => errors += "'port' field missing."; 0
      case Some(data) => data
    }

} 
