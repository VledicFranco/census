/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import scala.concurrent.Future

import com.github.nscala_time.time.Imports._ 

import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import requests.Utils

object GCE extends {

  private var access_token: String = null

  private var token_expiration: Long = 0

  private val apiPrefix: String = s"https://www.googleapis.com/compute/v1/project/${conf.project_id}"

  private def getAccessToken (callback: String=>Unit): Unit = {
    if (access_token != null && System.currentTimeMillis < token_expiration) {
      callback(access_token)
      return
    }
    WS.url(s"http://metadata/computeMetadata/v1/instance/service-accounts/default/token")
      .withHeaders("X-Google-Metadata-Request" -> "True")
      .get map { response => 
        access_token = (response.json \ "access_token").as[String] 
        token_expiration = System.currentTimeMillis - 1000 + (response.json \ "expires_in").as[Long]
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

  def createInstance (callback: Instance=>Unit): Unit = {
    getAccessToken { token =>
      WS.url(s"$apiPrefix/zones/${conf.zone}/instances")
        .withHeaders("Authorization" -> s"OAuth $token",
                     "Host" -> "www.googleapis.com",
                     "Content-Type" -> "application/json",
                     "User-Agent" -> "google-api-java-client/1.0")
        .post(Json.obj(
          "name" -> s"census-engine:${Utils.genUUID}",
          "machineType" -> conf.census_engine_machine_type,
          "networkInterfaces" -> Json.arr(Json.obj(
            "accessConfigs" -> Json.arr(Json.obj(
              "type" -> "ONE_TO_ONE_NAT",
              "name" -> "External NAT"
            )),
            "network" -> s"$apiPrefix/global/networks/default"
          )),
          "disk" -> Json.arr(Json.obj(
            "autoDelete" -> "false",
            "soruce" -> s"$apiPrefix/zones/${conf.zone}/disks/census-engine-disk",
            "boot" -> "true"
          )),
          "serviceAccounts" -> Json.arr(Json.obj(
            "scopes" -> Json.arr(
              "https://www.googleapis.com/auth/devstorage.read_only"
            )
          )),
          "metadata" -> Json.obj(
            "items" -> Json.arr(Json.obj(
              "key" -> "startup-script-url",
              "value" -> conf.census_engine_startup_script
            ))
          )
        ) 
      ) map { response =>
        for (ip <- (response.json \ "networkInterfaces" \\ "networkIP")) {
          val instance = new Instance(ip.as[String], conf.census_engine_port)
          instance.initialize(callback)
        }
      } recover {
        case _ => println(s"${DateTime.now} - ERROR: Couldn't reach the Google Compute Engine service.")
      }
    }
  }

}
