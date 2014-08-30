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

import org.addition.epanet.hydraulic.structures.SimulationNode

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
class SimulatedNode(node: SimulationNode) extends SimulatedElement {
  
  val elementType = NetworkElement.Node
  
  val simNode = node // keep track of Epanet simulation node
  
  var head = new AnalogValue[Length]("Head") // Epanet 'H[n]' variable, node head.
  var demand = new AnalogValue[VolumeFlowRate]("Demand") // Epanet 'D[n]' variable, node demand.
  var emitter = new AnalogValue[VolumeFlowRate]("Emitter") // Epanet 'E[n]' variable, emitter flows

  /**
   * Pretty print to a string the node description
   */
  override def toString() = {
    ", head=(" + head.computedValue + "->" + head.sensorValue +
      "), demand=(" + (demand.computedValue toString CubicMetersPerDay) +
      "->" + (demand.sensorValue toString CubicMetersPerDay) + ")"
  }
}