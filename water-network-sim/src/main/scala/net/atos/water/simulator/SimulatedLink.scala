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

import squants.Quantity
import squants.space.Length
import squants.space.Meters
import squants.space.Feet
import squants.motion.VolumeFlowRate
import squants.motion.VolumeFlowRateUnit
import squants.motion.CubicMetersPerSecond

/**
 *
 */
class SimulatedLink extends SimulatedElement {

  /**
   * Scale version of the Java Link StatType 
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

  var first: SimulatedNode = null
  var second: SimulatedNode = null
  var status = new DigitalValue[LinkStatus.Value] // Epanet 'S[k]', link current status
  var flow = new AnalogValue[VolumeFlowRate] // Epanet ''Q[k]', link flow value

  /**
   * Pretty print to a string the node description
   */
  override def toString() = {
    ", flow=(" + (flow.computedValue toString CubicMetersPerDay) +
      "->" + (flow.sensorValue toString CubicMetersPerDay) + ")"
  }
}