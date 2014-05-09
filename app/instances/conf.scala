/**
 * @author Francisco Miguel Arámburo Torres _ atfm05@gmail.com
 */

package instances

object conf {

  val project_id: String = "vidtecci-cloud"

  val zone: String = "us-central1-a"

  val census_engine_machine_type: String = "f1-micro"

  val census_engine_startup_script: String = "gs://census-framework/engine-startup.sh"

  val census_engine_port: Int = 80

  val census_control_host: String = "census-control"

  val census_control_port: Int = 9595

  val max_instances: Int = 5

  val ce_max_queue_size: Int = 5

  val balancing_heuristic: String = "next-free-instance"

}
