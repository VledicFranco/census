/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package control

import scala.collection.mutable.Queue

trait QueueAllPair extends QueueFiller {

  protected val requestsQueue = Queue[EngineRequest]() 

  def fillQueue = {}

}
