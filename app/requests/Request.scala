/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import scala.collection.mutable.List
import play.api.libs.json._

/**
 * An in request to be validated.
 */
trait Request {

  /** Used to store all the validation errors. */
  var errors = MutableList[String]()

  /**
   * Checks if there were any errors on the validation stage.
   *
   * @return 'true' if there were errors.
   *         'false' if there were no errors.
   */
  def hasErrors: Boolean = errors.length > 0

  /**
   * Converts all the errors to a json error report.
   *
   * @return a json object with the errors report.
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
