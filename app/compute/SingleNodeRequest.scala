/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package compute

import com.github.nscala_time.time.Imports._ 

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

trait SingleNodeRequest extends EngineRequest with Sender {

  val parent: MultiNodeRequest

  override def complete: Unit = parent.singleNodeFinished

}