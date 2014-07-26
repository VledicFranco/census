/**
 * @author Francisco Miguel Arámburo Torres _ atfm05@gmail.com
 */

package control

/**
 * Default configuration for the GCE flow.
 */
object conf {

  /** Google Compute Engine project id. */
  val project_id: String = "vidtecci-cloud"

  /** Zone on which the Google Comput Engine API will create the virtual servers. */
  val zone: String = "us-central1-a"

  /** Machine type of the virtual servers. */
  val census_engine_machine_type: String = "n1-highcpu-8"

  /** Location of the startup script for the Census Engine instances. */
  val census_engine_startup_script: String = "gs://census-framework/engine-startup.sh"
  
  /** Name of the snapshot from which the virtual servers will create their boot disks. */
  val census_engine_snapshot: String = "census-engine-snapshot"

  /** Hostname where the Census Control server is running. */
  val census_control_host: String = "census-control"

  /** Port used for every Census server. */
  val census_port: Int = 9000

}
