/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package controllers.requests

import play.api.libs.json._

import utils.Utils

/**
 * An in request to be validated.
 */
trait Request {

  /** A UUID string used to identify the request. */
  val token: String = Utils.genUUID
  
  /** Array used to store all the validation errors. */
  var errors: Array[String] = Array()

  /** 
   * Called to check if the json of the request is valid.
   */
  def validate: Unit

  /**
   * Request execution.
   */
  def start: Unit

  /**
   * Invoked by the object companions to start the request.
   */
  def execute: Unit = {
    if (!hasErrors) start
  }
  
  /**
   * Checks if there were any errors on the validation stage.
   *
   * @return 'true' if there were errors.
   *         'false' if there were no errors.
   */
  def hasErrors: Boolean = {
    errors.length > 0
  }

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
