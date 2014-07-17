/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package engine

import http.OutReports
import requests.EngineComputeRequest

/**
 * This trait has the interfaces and functionality needed for
 * a graph algorithm computation process.
 */
trait GraphCompute {

  /** Changed to 'true' only if the graph was succesfully imported. */
  var computationReady: Boolean = false

  /**
   * Used to setup the signalcollect's graph by adding all the 
   * imported vertices and edges before every computation.
   *
   * @param variables received for this execution.
   */
  def reset (variables: Array[String]): Unit

  /** 
   * Runs the algorithm.
   *
   * @param computationRequest for this execution.
   * @param variables received for this execution.
   */
  def computeExecute (computationRequest: EngineComputeRequest, variables: Array[String]): Unit

  /** 
   * Used by a EngineComputeRequest to start the algorithm.
   *
   * @param computationRequest for this execution.
   * @param variables received for this execution.
   */
  def computeStart (computationRequest: EngineComputeRequest, variables: Array[String]): Unit = {
    if (!computationReady) {
      OutReports.Error.computationNotReady(computationRequest)
      computationRequest.finish()
      return
    }
    reset(variables)
    computationRequest.computationTime = System.currentTimeMillis
    computeExecute(computationRequest, variables)
  }

  /** 
   * Invoked when the computation finishes, registers computation time,
   * and reports success or failure, then lets the next computation in the
   * queue begin.
   *
   * @param computationRequest for this execution.
   * @param success 'true' if the computation was successful.
   */
  def computeFinish (computationRequest: EngineComputeRequest, success: Boolean): Unit = {
    computationRequest.computationTime = System.currentTimeMillis - computationRequest.computationTime
    if (success) OutReports.Report.computationFinished(computationRequest)
    else OutReports.Error.computationFailed(computationRequest)
    computationRequest.finish()
  }

}
