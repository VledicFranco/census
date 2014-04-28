/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package instances

import controllers.WebService
import compute.EngineAlgorithm

class Instance (val host: String, val port: Int) extends WebService {

  this.setHost(host, port)
  
  /** Queue for the requests. */
  var queue: Queue[EngineAlgorithm] = Queue()

}
