/**
 * @author Francisco Miguel Arámburo Torres _ atfm05@gmail.com
 */

package instances

object conf {

  val census_control_host: String = "127.0.0.1"

  val census_control_port: Int = 9595

  val max_instances: Int = 5

  val ce_max_queue_size: Int = 5

  val balancing_heuristic: String = "next-free-instance"

}
