/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import com.github.nscala_time.time.Imports._ 

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

trait MultiNodeRequest[N] extends EngineRequest {

  val numNodes: Int

  def send (instance: Instance) = {}

}
