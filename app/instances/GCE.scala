/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import scala.concurrent.Future

import com.github.nscala_time.time.Imports._ 

import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import controllers.WebService

object GCE extends WebService {

  private var access_token: String = null

  private var token_expiration: Long = 0

  private def getAccessToken (callback: String=>Unit): Unit = {
    if (access_token != null && System.currentTimeMillis < token_expiration) return callback(access_token)
    WS.url(s"http://metadata/computeMetadata/v1/instance/service-accounts/default/token")
      .withHeaders("X-Google-Metadata-Request" -> "True")
      .get map { response => 
        (response.json \ "access_token").asOpt[String] match {
          case Some(token) => access_token = token
          case None => println(s"${DateTime.now} - ERROR: Json from Google metadata service is invalid, please check for bugs.")
        }
        (response.json \ "expires_in").asOpt[String] match {
          case Some(expiration) => token_expiration = expiration + System.currentTimeMillis - 1000
          case None => println(s"${DateTime.now} - ERROR: Json from Google metadata service is invalid, please check for bugs.")
        }
        callback(access_token)
      } recover {
        case _ => println(s"${DateTime.now} - ERROR: Couldn't reach the Google metadata service.")
      }
  }

  def verifyToken: Unit = {
    getAccessToken { token =>
      println(token)
    }
  }

//  override def post (path: String, data: String): Future[Response] = {
//      
//  }

}
