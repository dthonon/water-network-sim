/**
 * Exporting values to the modbus map
 *
 * This class transfer the sensor values to the modbus interface
 * For the moment, all values are stored in a single modbus map
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

import scala.collection.JavaConverters.asScalaBufferConverter

import net.wimpi.modbus.ModbusCoupler
import net.wimpi.modbus.net.ModbusTCPListener
import net.wimpi.modbus.procimg.SimpleProcessImage
import net.wimpi.modbus.procimg.SimpleRegister

import org.slf4j.LoggerFactory
import ch.qos.logback.core.util.StatusPrinter
import ch.qos.logback.classic.LoggerContext

class ModbusSlave {
  def logger = LoggerFactory.getLogger(this.getClass().getName())
  var spi: SimpleProcessImage = new SimpleProcessImage()
  var listener: ModbusTCPListener = null

  def initModbus(port: Int = 22225) {
    // Initializing modbus interface
    logger.info("Initializing modbus interface")
    //1. Basic variables
    //2. Prepare a process image
    //			spi.addDigitalOut(new SimpleDigitalOut(true))
    //			spi.addDigitalIn(new SimpleDigitalIn(true))
    //			spi.addInputRegister(new SimpleInputRegister(45))
    //3. Set the image on the coupler
    ModbusCoupler.getReference().setProcessImage(spi)
    ModbusCoupler.getReference().setMaster(false)
    ModbusCoupler.getReference().setUnitID(15)
    //4. Create a listener with 3 threads in pool
    listener = new ModbusTCPListener(3);
    listener.setPort(port);
    logger.info("Starting Modbus listener on " + port);
    listener.start();
    logger.debug("Created modbus interface")
  }

  def shutdownModbus {
    listener.stop();
  }

  def createRegisters(netw: NetworkDescription) {
    var reg = 0
    for ((nodeId, node) <- netw.SimulatedNodes) {
      // Create 2 registers in the modbus map
      logger.debug("Modbus register " + reg + " node head -> " + nodeId)
      spi.addRegister(new SimpleRegister(0))
      reg += 1
      logger.debug("Modbus register " + reg + " node demand -> " + nodeId)
      spi.addRegister(new SimpleRegister(0))
      reg += 1
    }
    for ((linkId, link) <- netw.SimulatedLinks) {
      // Create 2 registers in the modbus map
      logger.debug("Modbus register " + reg + " link status -> " + linkId)
      spi.addRegister(new SimpleRegister(0))
      reg += 1
      logger.debug("Modbus register " + reg + " link flow -> " + linkId)
      spi.addRegister(new SimpleRegister(0))
      reg += 1
    }
  }

  def updateRegisters(netw: NetworkDescription) {
    // Update Modbus map
    var reg = 0
    for ((nodeId, node) <- netw.SimulatedNodes) {
      val dem = Math.round(100 * node.demand.sensorValue.to(CubicMetersPerDay)).asInstanceOf[Int]
      logger.trace("Setting register " + reg + " to " + dem)
      spi.getRegister(reg).setValue(dem)
      reg += 1
      val head = Math.round(node.head.sensorValue.value).asInstanceOf[Int]
      logger.trace("Setting register " + reg + " to " + head)
      spi.getRegister(reg).setValue(head)
      reg += 1
    }
    for ((linkId, link) <- netw.SimulatedLinks) {
      logger.trace("Setting register " + reg + " to " + link.status.sensorValue.id)
      spi.getRegister(reg).setValue(link.status.sensorValue.id)
      reg += 1
      val flow = Math.round(100 * link.flow.sensorValue.to(CubicMetersPerDay)).asInstanceOf[Int]
      logger.trace("Setting register " + reg + " to " + flow)
      spi.getRegister(reg).setValue(flow)
      reg += 1
    }
  }
}