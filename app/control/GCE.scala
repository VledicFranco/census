/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control 

import scala.concurrent.Future

import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import shared.Utils
import shared.Log

/**
 * Module that handles the GCE http api.
 */
object GCE {

  /** Access token to authorize requests. */
  private var access_token: String = null

  /** Life length of the access token in milliseconds. */
  private var token_expiration: Long = 0

  /** URL prefix for GCE http api requests. */
  private val apiPrefix: String = s"https://www.googleapis.com/compute/v1/projects/${conf.project_id}"

  /** URL prefix for GCE http api requests with the project's zone. */
  private val apiPrefixWithZone: String = s"$apiPrefix/zones/${conf.zone}"

  /**
   * Method to test the obtention of an access token.
   */
  def verifyToken: Unit = {
    getAccessToken { token =>
      println(token)
    }
  }

  /**
   * Requests the creation of a GCE virtual machine.
   *
   * @param callback(String, String) function to be executed when the instance is ready.
   *                   ip   hostname 
   */
  def createInstance (callback: (String, String)=>Unit): Unit = {
    val instanceName: String = s"census-engine-${Utils.genUUID}" 
    val diskName: String = s"disk-$instanceName"
    createDiskRequest(diskName, { () =>
      createInstanceRequest(instanceName, diskName, { () =>
        getInstanceIp(instanceName, { ip =>
          callback(ip, instanceName)
        })
      })
    })
  }

  /**
   * Requests the deletion of a GCE virtual machine.
   *
   * @param host of the virtual machine to be deleted.
   * @param callback function to be executed when the instance is deleted.
   */
  def deleteInstance (host: String, callback: ()=>Unit): Unit = {
    deleteInstanceRequest(host, callback)
  }

  /**
   * Requests the creation of a bootable disk for a GCE virtual machine.
   *
   * @param diskName of the disk to be created.
   * @param callback function to be executed when the disk is created.
   */
  private def createDiskRequest (diskName: String, callback: ()=>Unit): Unit = {  
    authorizedPost(s"$apiPrefixWithZone/disks", createDiskPayload(diskName), { response =>
      Log.info(s"Creating $diskName.")
      checkOperation((response.json \ "selfLink").as[String], { () =>
        Log.info(s"$diskName created.")
        callback()
      }) 
    }) 
  }

  /**
   * The actual api call to create a GCE virtual machine.
   *
   * @param instanceName of the instance to be created.
   * @param diskName of the boot disk for the instance.
   * @param callback function to be executed when the request is done.
   */
  private def createInstanceRequest (instanceName: String, diskName: String, callback: ()=>Unit): Unit = {
    authorizedPost(s"$apiPrefixWithZone/instances", createInstancePayload(instanceName, diskName), { response =>
      Log.info(s"Creating $instanceName.")
      checkOperation((response.json \ "selfLink").as[String], { () =>
        Log.info(s"$instanceName created.")
        callback()
      }) 
    })
  }

  /**
   * Actual api call to delte a GCE virtual machine.
   *
   * @param instanceName of the virtual machine to be deleted.
   * @param callback function to be executed when the request is done.
   */
  private def deleteInstanceRequest (instanceName: String, callback: ()=>Unit): Unit = {
    authorizedDelete(s"$apiPrefixWithZone/instances/$instanceName", { response =>
      Log.info(s"Deleting $instanceName.")
      checkOperation((response.json \ "selfLink").as[String], { () =>
        Log.info(s"$instanceName deleted.")
        callback()
      }) 
    })
  }

  /**
   * Obtains the internal network ip of a GCE virtual machine.
   *
   * @param instanceName of the desired virtual machine.
   * @param callback(String) function to be executed when the ip is obtained.
   *                  ip
   */
  private def getInstanceIp (instanceName: String, callback: String=>Unit): Unit = {
    authorizedGet(s"$apiPrefixWithZone/instances/$instanceName", { response =>
      for (ip <- (response.json \ "networkInterfaces" \\ "networkIP")) {
        callback(ip.as[String])
      }     
    })
  }

  /**
   * Requests the GCE metadata service for a new api access token
   * only if the last one is expired or is about to expire.
   *
   * @param callback function to be executed when the token is obtained.
   */
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
        case _ => Log.error("Couldn't reach the Google metadata service.")
      }
  }

  /**
   * Makes a HTTP GET method with a GCE api authorization token.
   *
   * @param url where the request is going to be sent.
   * @param callback function to be executed when the request has responded.
   */
  private def authorizedGet (url: String, callback: Response=>Unit): Unit = {
    getAccessToken { token => 
      WS.url(url)
        .withHeaders("Authorization" -> s"OAuth $token")
        .get map { response =>
          validateAuthorizedRequest(url, response)
          callback(response)
      } recover {
        case _ => Log.error("Couldn't reach the Google Compute Engine service.")
      }
    } 
  }

  /**
   * Makes a HTTP POST method with a GCE api authorization token.
   *
   * @param url where the request is going to be sent.
   * @param callback function to be executed when the request has responded.
   */
  private def authorizedPost (url: String, data: JsValue, callback: Response=>Unit): Unit = {
    getAccessToken { token =>
      WS.url(url)
        .withHeaders("Authorization" -> s"OAuth $token", "Content-Type" -> "application/json")
        .post(data) map { response =>
          validateAuthorizedRequest(url, response)
          callback(response)
      } recover {
        case _ => Log.error("Couldn't reach the Google Compute Engine service.")
      }
    }    
  }

  /**
   * Makes a HTTP DELETE method with a GCE api authorization token.
   *
   * @param url where the request is going to be sent.
   * @param callback function to be executed when the request has responded.
   */
  private def authorizedDelete (url: String, callback: Response=>Unit): Unit = {
    getAccessToken { token =>
      WS.url(url)
        .withHeaders("Authorization" -> s"OAuth $token", "Content-Type" -> "application/json")
        .delete map { response =>
          validateAuthorizedRequest(url, response)
          callback(response)
      } recover {
        case _ => Log.error("Couldn't reach the Google Compute Engine service.")
      }
    }
  }

  /**
   * Checks if the response of an authorized request was successful.
   *
   * @param url to log if it wasn't successful.
   * @param response to be checked.
   */
  private def validateAuthorizedRequest (url: String, response: Response): Unit = {
    if (response.status != 200) {
      Log.error(s"$url response status ${response.status}, printing json:")
      println(response.json)
    }
  }

  /**
   * Checks if a GCE api call operation is done.
   *
   * @param link of the operation.
   * @param callback function to be executed when the operation is done.
   */
  private def checkOperation (link: String, callback: ()=>Unit): Unit = {
    authorizedGet(link, { response => 
      if ((response.json \ "status").as[String] == "DONE") {
        callback()
      } else {
        Thread.sleep(3000)
        checkOperation(link, callback) 
      }
    })
  }

  /**
   * Creates the json necessary to create a GCE virtual machine boot disk.
   *
   * @param diskName to be used for the creation.
   * @return the json for the api request.
   */
  private def createDiskPayload (diskName: String): JsValue = {
    Json.obj(
      "kind" -> "compute#disk",
      "name" -> diskName,
      "zone" -> apiPrefixWithZone,
      "description" -> "Persistent boot disk.",
      "sourceSnapshot" -> s"$apiPrefix/global/snapshots/${conf.census_engine_snapshot}"
    )
  }

  /**
   * Creates the json necessary to create a GCE virtual machine.
   *
   * @param instanceName to be used for the creation.
   * @param diskName to be used for the creation.
   * @return the json for the api request.
   */
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

}
