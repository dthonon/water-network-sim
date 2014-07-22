/**
 * Driver for Water Network Simulator
 * This driver drives the network simulation, sensor simulation and modbus interface
 *
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

import java.io.File

import scala.collection.JavaConverters.asScalaBufferConverter

import org.addition.epanet.hydraulic.HydraulicSim
import org.addition.epanet.network.Network
import org.addition.epanet.network.io.input.InputParser

import grizzled.slf4j.Logging

object NetworkSimulator extends Logging {

  def main(args: Array[String]): Unit = {
    val inFile = args(0)
    val maxCycles = args(1).toInt
 
    // Create store for the network values 
    val netw = new NetworkDescription(inFile)
    netw.createNodes
    
    // Create the modbus slave and add registers
    val modbus = new ModbusSlave
    modbus.initModbus
    modbus.createRegisters(netw)

    for (i <- 1 to maxCycles) {
      // Run one simulation step
      netw.simStep
      
      // Simulate sensors
      netw.simSensors

      // Print simulated state
      info(netw.toString)

      // Update modbus map
      modbus.updateRegisters(netw)
      
      Thread.sleep(1000)
      info("Waited 1s")
    }

    modbus.shutdownModbus
    info("Stopped modbus listener")
    System.exit(0)

  }
}
