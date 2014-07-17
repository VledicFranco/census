/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package requests

import play.api.libs.json._

import shared.Utils

/**
 * An in request to be validated.
 */
trait Request {

  /** Request flow. */
  validate
  execute

  /** A UUID string used to identify the request. */
  var token: String = Utils.genUUID

  /** Array used to store all the validation errors. */
  var errors: Array[String] = Array()

  /**
   * Request execution.
   */
  def body: Unit

  /** 
   * Called to check if the json of the request is valid.
   */
  def validate: Unit

  /**
   * Invoked by the object companions to start the request.
   */
  def execute: Unit = if (!hasErrors) body

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
