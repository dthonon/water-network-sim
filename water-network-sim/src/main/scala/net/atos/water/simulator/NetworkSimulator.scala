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

import net.wimpi.modbus.Modbus

import org.addition.epanet.hydraulic.HydraulicSim
import org.addition.epanet.network.Network
import org.addition.epanet.network.io.input.InputParser

import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory
import ch.qos.logback.core.util.StatusPrinter
import ch.qos.logback.classic.LoggerContext

/**
 * Configuration of the command line parameters
 */
case class Config(
  inFile: String = "",
  maxCycles: Int = 10,
  stepTime: Int = 1,
  logFile: String = "",
  modbusMap: String = "",
  port: Int = Modbus.DEFAULT_PORT,
  verbose: Boolean = true,
  debug: Boolean = false,
  trace: Boolean = false)

/**
 * Main
 */
object NetworkSimulator {

  def main(args: Array[String]): Unit = {

    // Logger for this class
    def logger: ch.qos.logback.classic.Logger =
      LoggerFactory.getLogger(this.getClass().getName()).asInstanceOf[ch.qos.logback.classic.Logger]
    // Root logger, used to set logging level for all loggers
    def rootLogger: ch.qos.logback.classic.Logger =
      LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
    rootLogger.setLevel(Level.INFO)

    //StatusPrinter.print((LoggerFactory.getILoggerFactory).asInstanceOf[LoggerContext])
    logger.info("Starting")

    // Evaluate the command line parameters
    val parser = new scopt.OptionParser[Config]("NetworkSimulator") {
      head("NetworkSimulator", "0.x")
      opt[String]('i', "inFile") required () valueName ("<file>") action { (x, c) =>
        c.copy(inFile = x)
      } text ("EPAnet input file (required)")
      opt[Int]('m', "maxCycles") action { (x, c) =>
        c.copy(maxCycles = x)
      } text ("maxCycles indicates the number of simulation cycles")
      opt[Int]('s', "stepTime") action { (x, c) =>
        c.copy(stepTime = x)
      } text ("stepTime indicates number of sec. of each cycle")
      opt[String]('i', "logFile") valueName ("<file>") action { (x, c) =>
        c.copy(logFile = x)
      } text ("File logging the simulation step results")

      opt[String]('b', "modbusMap") valueName ("<file>") action { (x, c) =>
        c.copy(modbusMap = x)
      } text ("Output file listing modbus map")
      opt[Int]('p', "port") action { (x, c) =>
        c.copy(port = x)
      } text ("Modbus slave TCP/IP port")

      opt[Unit]("verbose") action { (_, c) =>
        c.copy(verbose = true)
      } text ("Log main simulation steps")
      opt[Unit]("debug") hidden () action { (_, c) =>
        c.copy(debug = true)
      } text ("Log detailed simulation steps (lot of text)")
      opt[Unit]("trace") hidden () action { (_, c) =>
        c.copy(trace = true)
      } text ("Log very detailed simulation steps (lot of text)")

      note("NetworkSimulation hydraulic network simulation\n")
      help("help") text ("prints this usage text")
    }

    // parser.parse returns Option[C]
    parser.parse(args, Config()) map { config =>

      // Setting logging level
      if (config.verbose ) rootLogger.setLevel(Level.INFO)
      if (config.debug ) rootLogger.setLevel(Level.DEBUG)
      if (config.trace ) rootLogger.setLevel(Level.TRACE)

      // Create store for the network values 
      val netw = new NetworkDescription(config.inFile, config.logFile )
      netw.createNodes
      netw.createLinks

      // Create the modbus slave and add registers
      val modbus = new ModbusSlave
      modbus.initialize(config.port)
      modbus.createRegisters(netw, config.modbusMap)

      for (i <- 1 to config.maxCycles) {
        logger.info("Running simulation step " + i + " after " + config.stepTime + " sec.")
        // Run one simulation step
        netw.simNetwork

        // Simulate sensors
        netw.simSensors

        // Print simulated state
        if (logger.isTraceEnabled()) logger.trace(netw.toString)

        // Retrieve modbus map and look for commands from master
        // after the first step
        if (i > 1) {
          modbus.readRegisters(netw)
        }
        
        // Update modbus map
        modbus.updateRegisters(netw)

        Thread.sleep(1000 * config.stepTime)
      }

      modbus.shutdown
      netw.shutdown
      logger.info("Stopped network simulator and modbus interface")
      System.exit(0)

    } getOrElse {
      // arguments are bad, error message will have been displayed
      logger.error("Bad arguments")
    }

  }
}
