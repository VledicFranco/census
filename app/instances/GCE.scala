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

  private val apiPrefix: String = s"https://www.googleapis.com/compute/v1/projects/${conf.project_id}"

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
      val instanceID: String = s"census-engine-${Utils.genUUID}" 
      WS.url(s"$apiPrefix/zones/${conf.zone}/instances")
        .withHeaders("Authorization" -> s"OAuth $token",
                     "Host" -> "www.googleapis.com",
                     "Content-Type" -> "application/json")
        .post(Json.obj(
          "name" -> instanceID,
          "machineType" -> s"$apiPrefix/zones/${conf.zone}/machineTypes/${conf.census_engine_machine_type}",
          "networkInterfaces" -> Json.arr(Json.obj(
            "accessConfigs" -> Json.arr(Json.obj(
              "type" -> "ONE_TO_ONE_NAT",
              "name" -> "External NAT"
            )),
            "network" -> s"$apiPrefix/global/networks/census-framework"
          )),
          "disks" -> Json.arr(Json.obj(
            "autoDelete" -> "false",
            "source" -> s"$apiPrefix/zones/${conf.zone}/disks/census-engine-disk",
            "boot" -> "true"
          )),
          "serviceAccounts" -> Json.arr(Json.obj(
            "email" -> "default",
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
        checkOperation((response.json \ "selfLink").as[String], instanceID, callback) 
      } recover {
        case _ => println(s"${DateTime.now} - ERROR: Couldn't reach the Google Compute Engine service.")
      }
    }
  }

  def checkOperation (link: String, instanceID: String, callback: Instance=>Unit): Unit = {
    getAccessToken { token => 
      WS.url(link)
        .withHeaders("Authorization" -> s"OAuth $token")
        .get map { response => 
          if ((response.json \ "status").as[String] == "DONE") {
            println(s"${DateTime.now} - INFO: $instanceID created.")
            getInstance(instanceID, callback)
          } else {
            println(s"${DateTime.now} - INFO: Instance $instanceID still not ready, will wait 3 seconds.")
            Thread.sleep(3000)
            checkOperation(link, instanceID, callback) 
          }
      } recover {
        case _ => println(s"${DateTime.now} - ERROR: Couldn't reach the Google Compute Engine service operation request.")
      } 
    }
  }

  def getInstance (instanceID: String, callback: Instance=>Unit):Unit = {
    getAccessToken { token =>  
      WS.url(s"$apiPrefix/zones/${conf.zone}/instances/$instanceID")
        .withHeaders("Authorization" -> s"OAuth $token")
        .get map { response =>
          for (ip <- (response.json \ "networkInterfaces" \\ "networkIP")) {
            val instance = new Instance(ip.as[String], conf.census_engine_port, instanceID)
            instance.initialize(callback)
          }
      } recover {
        case _ => println(s"${DateTime.now} - ERROR: Couldn't reach the Google Compute Engine service instance request.")
      }
    }
  }
}
