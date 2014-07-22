/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.collection.mutable.Queue

trait QueueFiller {

  protected val requestsQueue: Queue[EngineRequest]

  protected def fillingFinished: Unit

  def fillQueue: Unit

}
