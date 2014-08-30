/**
 * A node of the network
 *
 * water-network-sim
 * Copyright (c) 2014, Daniel Thonon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.atos.water.simulator

import org.addition.epanet.hydraulic.structures.SimulationLink
import org.addition.epanet.network.structures.Link

import squants.Quantity
import squants.space.Length
import squants.space.Meters
import squants.space.Feet
import squants.motion.VolumeFlowRate
import squants.motion.VolumeFlowRateUnit
import squants.motion.CubicMetersPerSecond

import org.slf4j.LoggerFactory
import ch.qos.logback.core.util.StatusPrinter
import ch.qos.logback.classic.LoggerContext

/**
 * Scala version of the Java Link StatType
 */
object LinkStatus extends Enumeration {
  /**
   * Valve active (partially open).
   */
  val ACTIVE = Value(4, "ACTIVE")
  /**
   * Closed.
   */
  val CLOSED = Value(2, "CLOSED")
  /**
   * Tank emptying.
   */
  val EMPTYING = Value(9, "EMPTYING")
  /**
   * Tank filling.
   */
  val FILLING = Value(8, "FILLING")
  /**
   * Open.
   */
  val OPEN = Value(3, "OPEN")
  /**
   * Temporarily closed.
   */
  val TEMPCLOSED = Value(1, "TEMPCLOSED")
  /**
   * FCV cannot supply flow.
   */
  val XFCV = Value(6, "XFCV")
  /**
   * Pump exceeds maximum flow.
   */
  val XFLOW = Value(5, "XFLOW")
  /**
   * Pump cannot deliver head (closed).
   */
  val XHEAD = Value(0, "XHEAD")
  /**
   * Valve cannot supply pressure.
   */
  val XPRESSURE = Value(7, "XPRESSURE")

}

/**
 * Scala version of Type of link
 */
object LinkType extends Enumeration {
  /**
   * Pipe with check valve.
   */
  val CV = Value(0, "CV")
  /**
   * Flow control valve.
   */
  val FCV = Value(6, "FCV")
  /**
   * General purpose valve.
   */
  val GPV = Value(8, "GPV")
  /**
   * Pressure breaker valve.
   */
  val PBV = Value(5, "PBV")
  /**
   * Regular pipe.
   */
  val PIPE = Value(1, "PIPE")
  /**
   * Pressure reducing valve.
   */
  val PRV = Value(3, "PRV")
  /**
   * Pressure sustaining valve.
   */
  val PSV = Value(4, "PSV")
  /**
   * Pump.
   */
  val PUMP = Value(2, "PUMP")
  /**
   * Throttle control valve.
   */
  val TCV = Value(7, "TCV")
}

/**
 * Link, interface to EPAnet Link
 */
class SimulatedLink(link: SimulationLink) extends SimulatedElement {

  def logger = LoggerFactory.getLogger(this.getClass().getName())

  val elementType = NetworkElement.Link

  val simLink = link // Keep track of Epanet simulation link
  
//  var first: SimulatedNode = null
//  var second: SimulatedNode = null
  var status = new DigitalValue[LinkStatus.Value]("Status") // Epanet 'S[k]', link current status
  var flow = new AnalogValue[VolumeFlowRate]("Flow") // Epanet ''Q[k]', link flow value

  /**
   * Override status setter to push new state to network simulator
   * @param newStatus updated status
   */
  def status_=(newStatus: LinkStatus.Value) {
    logger.debug("Changing link status from " + status.computedValue + 
        " to " + newStatus + ", Epanet StatType = " + Link.StatType.valueOf(newStatus.toString()))
    status.computedValue = newStatus
    simLink.setSimStatus(Link.StatType.valueOf(newStatus.toString()))
    
  }
  /**
   * Pretty print to a string the node description
   */
  override def toString() = {
    ", flow=(" + (flow.computedValue toString CubicMetersPerDay) +
      "->" + (flow.sensorValue toString CubicMetersPerDay) + "), status=(" +
      status.computedValue + "->" + status.sensorValue + ")"
  }
}