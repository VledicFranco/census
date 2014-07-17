/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

import shared.Neo4j
import engine.GraphImport

/**
 * Neo4j singleton for Census Engine.
 */
object DB extends Neo4j {

  /** Reference to the current imported graph. */
  var importedGraphFormat: GraphImport = null

}
