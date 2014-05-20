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

  val token: String = Utils.genUUID
  
  /** Array used to store all the validation errors. */
  var errors: Array[String] = Array()

  /** 
   */
  def validate: Unit

  def start: Unit

  def execute: Unit = {
    if (!hasErrors) start
  }
  
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
