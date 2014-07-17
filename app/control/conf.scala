/**
 * @author Francisco Miguel Arámburo Torres _ atfm05@gmail.com
 */

package control

/**
 * Default configuration for the GCE flow.
 */
object conf {

  val project_id: String = ""

  val zone: String = "us-central1-a"

  val census_engine_machine_type: String = "n1-highcpu-8"

  val census_engine_startup_script: String = "gs://census-framework/engine-startup.sh"
  
  val census_engine_snapshot: String = "census-engine-snapshot"

  val census_control_host: String = "census-control"

  val census_port: Int = 9595

}
