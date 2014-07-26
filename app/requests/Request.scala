/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import scala.collection.mutable.MutableList

import play.api.libs.json._

/** A request to be validated. */
trait Request {

  /** A unique identifier for the request. */
  val token: String

  /** Stores all the validation errors. */
  var errors = MutableList[String]()

  /** Checks if there were any errors on the validation stage.
    *
    * @return 'true' if there were errors.
    *         'false' if there were no errors.
    */
  def hasErrors: Boolean = errors.length > 0

  /** Converts all the errors to a json payload.
    *
    * @return a json object with all the errors.
    */
  def errorsToJson: JsValue = {
    if (errors.length == 0)
      Json.obj("errors" -> 0)
    else
      Json.obj(
        "status" -> "bad request",
        "errors" -> Json.toJson(errors)
      )
  }

}
