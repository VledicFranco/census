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

object GCE {

  private var access_token: String = null

  private var token_expiration: Long = 0

  private val apiPrefix: String = s"https://www.googleapis.com/compute/v1/projects/${conf.project_id}"

  private val apiPrefixWithZone: String = s"$apiPrefix/zones/${conf.zone}"

  def verifyToken: Unit = {
    getAccessToken { token =>
      println(token)
    }
  }

  def createInstance (callback: (String, Int)=>Unit): Unit = {
    val instanceName: String = s"census-engine-${Utils.genUUID}" 
    val diskName: String = s"disk-$instanceName"
    createDiskRequest(diskName, { () =>
      createInstanceRequest(instanceName, diskName, { () =>
        callback(instanceName, conf.census_engine_port)
      })
    })
  }

  def deleteInstance (instanceName: String, callback: ()=>Unit): Unit = {
    deleteInstanceRequest(instanceName, callback)
  }

  private def createDiskRequest (diskName: String, callback: ()=>Unit): Unit = {  
    authorizedPost(s"$apiPrefixWithZone/disks", createDiskPayload(diskName), { response =>
      checkOperation((response.json \ "selfLink").as[String], { () =>
        println(s"${DateTime.now} - INFO: $diskName created.")
        callback()
      }) 
    }) 
  }

  private def createInstanceRequest (instanceName: String, diskName: String, callback: ()=>Unit): Unit = {
    authorizedPost(s"$apiPrefixWithZone/instances", createInstancePayload(instanceName, diskName), { response =>
      checkOperation((response.json \ "selfLink").as[String], { () =>
        println(s"${DateTime.now} - INFO: $instanceName created.")
        callback()
      }) 
    })
  }

  private def deleteInstanceRequest (instanceName: String, callback: ()=>Unit): Unit = {
    authorizedDelete(s"$apiPrefixWithZone/instances/$instanceName", { response =>
      checkOperation((response.json \ "selfLink").as[String], { () =>
        println(s"${DateTime.now} - INFO: $instanceName deleted.")
        callback()
      }) 
    })
  }

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

  private def authorizedGet (url: String, callback: Response=>Unit): Unit = {
    getAccessToken { token => 
      WS.url(url)
        .withHeaders("Authorization" -> s"OAuth $token")
        .get map { response =>
          validateAuthorizedRequest(url, response, { () => callback(response) })
      } recover {
        case _ => println(s"${DateTime.now} - ERROR: Couldn't reach the Google Compute Engine service.")
      }
    } 
  }

  private def authorizedPost (url: String, data: JsValue, callback: Response=>Unit): Unit = {
    getAccessToken { token =>
      WS.url(url)
        .withHeaders("Authorization" -> s"OAuth $token", "Content-Type" -> "application/json")
        .post(data) map { response =>
          validateAuthorizedRequest(url, response, { () => callback(response) })
      } recover {
        case _ => println(s"${DateTime.now} - ERROR: Couldn't reach the Google Compute Engine service.")
      }
    }    
  }

  private def authorizedDelete (url: String, callback: Response=>Unit): Unit = {
    getAccessToken { token =>
      WS.url(url)
        .withHeaders("Authorization" -> s"OAuth $token", "Content-Type" -> "application/json")
        .delete map { response =>
          validateAuthorizedRequest(url, response, { () => callback(response) })
      } recover {
        case _ => println(s"${DateTime.now} - ERROR: Couldn't reach the Google Compute Engine service.")
      }
    }
  }

  private def validateAuthorizedRequest (url: String, response: Response, callback: ()=>Unit): Unit = {
    if (response.status != 200) {
      println(s"${DateTime.now} - ERROR: $url response status ${response.status}, printing json:")
      println(response.json)
    }
  }

  private def checkOperation (link: String, callback: ()=>Unit): Unit = {
    authorizedGet(link, { response => 
      if ((response.json \ "status").as[String] == "DONE") {
        callback()
      } else {
        println(s"${DateTime.now} - INFO: Operation $link still not ready, will wait 3 seconds.")
        Thread.sleep(3000)
        checkOperation(link, callback) 
      }
    })
  }

  private def createDiskPayload (diskName: String): JsValue = {
    Json.obj(
      "kind" -> "compute#disk",
      "name" -> diskName,
      "zone" -> apiPrefixWithZone,
      "description" -> "Persistent boot disk.",
      "sourceSnapshot" -> s"$apiPrefix/global/snapshots/${conf.census_engine_snapshot}"
    )
  }

  private def createInstancePayload (instanceName: String, diskName: String): JsValue = {
    Json.obj(
      "name" -> instanceName,
      "machineType" -> s"$apiPrefixWithZone/machineTypes/${conf.census_engine_machine_type}",
      "tags" -> Json.obj(
        "items" -> Json.arr(
          "census-engine-instance"
        )
      ),
      "networkInterfaces" -> Json.arr(Json.obj(
        "accessConfigs" -> Json.arr(Json.obj(
          "type" -> "ONE_TO_ONE_NAT",
          "name" -> "External NAT"
        )),
        "network" -> s"$apiPrefix/global/networks/census-framework"
      )),
      "disks" -> Json.arr(Json.obj(
        "kind" -> "compute#attachedDisk",
        "boot" -> true,
        "type" -> "PERSISTENT",
        "mode" -> "READ_WRITE",
        "autoDelete" -> true,
        "deviceName" -> diskName,
        "zone" -> apiPrefixWithZone,
        "source" -> s"$apiPrefixWithZone/disks/$diskName"
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
  }

/*
 *  def getInstance (instanceName: String, callback: Instance=>Unit):Unit = {
 *    getAccessToken { token =>  
 *      WS.url(s"$apiPrefixWithZone/instances/$instanceName")
 *        .withHeaders("Authorization" -> s"OAuth $token")
 *        .get map { response =>
 *          // response.json
 *      } recover {
 *        case _ => println(s"${DateTime.now} - ERROR: Couldn't reach the Google Compute Engine service instance request.")
 *      }
 *    }
 *  }
 */

}
