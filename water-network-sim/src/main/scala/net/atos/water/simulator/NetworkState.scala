/**
 * Description of the network state: nodes, links...
 *
 * This class contains the full description of the network and the methods to work on it
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

import scala.collection.mutable.Map
import scala.collection.JavaConverters.asScalaBufferConverter

import grizzled.slf4j.Logging

import org.addition.epanet.hydraulic.HydraulicSim
import org.addition.epanet.network.Network
import org.addition.epanet.network.io.input.InputParser

import java.io.File

import squants.Quantity
import squants.space.Length
import squants.space.Meters
import squants.space.Feet
import squants.motion.VolumeFlowRate
import squants.motion.VolumeFlowRateUnit
import squants.motion.CubicMetersPerSecond

object CubicMetersPerDay extends VolumeFlowRateUnit {
    val symbol = "m³/d"
    val multiplier = CubicMetersPerSecond.multiplier / (60 * 60 * 24)
}
  

/**
 *  Description of the water network: nodes, links...
 *  Each type is described in an inner class and objects are stored in a map
 *
 */
class NetworkDescription (inputFile : String) extends Logging {
  
  val inFile = new File(inputFile)

  // Epanet values
  val net = new Network
  val julLogger =
    java.util.logging.Logger.getLogger("networkSimulator")

  info("Initializing simulation engine Epanet with file: " + inputFile)
  // Parse Epanet input file
  val parserINP = InputParser.create(Network.FileType.INP_FILE, julLogger)
  parserINP.parse(net, inFile);
  info(s"Opened net file")
  val pMap = net.getPropertiesMap();
  // run infinite
  net.getPropertiesMap().setDuration(java.lang.Long.MAX_VALUE);
  // Create simulation
  val hydSim = new HydraulicSim(net, julLogger);
  info("Created Epanet simulation")

  /**
   * Any type of value, containing
   * - the computed value, resulting from the simulation
   * - the measured value, after sensor and acquisition processing
   * - the ref to the sensor
   *
   */
  class SimulatedValue[A <: Quantity[A]] {
    var computedValue: A = _ // "Exact" value given by Epanet simulator
    var sensorValue: A = _ // Value given by the sensor (+noise, drift...)
    val sensor = new SensorSimulation[A]
  }

  /**
   * A  node of the network
   *
   */
  class SimulatedNode {
    var head = new SimulatedValue[Length] // Epanet 'H[n]' variable, node head.
    var demand = new SimulatedValue[VolumeFlowRate] // Epanet 'D[n]' variable, node demand.
    var emitter = new SimulatedValue[VolumeFlowRate] // Epanet 'E[n]' variable, emitter flows

    /**
     * Pretty print to a string the node description
     */
    override def toString() = {
      ", head=(" + head.computedValue + "->" + head.sensorValue +
        "), demand=(" + (demand.computedValue toString CubicMetersPerDay) + 
        "->" + (demand.sensorValue toString CubicMetersPerDay) + ")"
    }
  }

  // The map listing the nodes
  var SimulatedNodes: Map[String, SimulatedNode] = Map()

  /**
   * Create a new node and adds it to the nodes map
   *
   * @constructor create a new node with its id and empty values
   * @param id the node identifier, key in the map
   * @return the new node
   */
  def newNode(id: String): SimulatedNode = {
    val res = new SimulatedNode
    SimulatedNodes.+=(id -> res)
    res
  }

  /**
   * Create the nodes, based on Epanet network description
   */
  def createNodes {
    for (node <- hydSim.getnNodes().asScala) {
      // Create a node in the network description
      val simNode = newNode(node.getId())
      info("Network state node ->" + node.getId())
    }
  }
  /**
   * Simulate one Epanet step
   */
  def simStep {
    var delay = hydSim.simulateSingleStep();
    info("Stepped for " + delay + " sec., Simulation time: " + hydSim.getHtime())

    // Transfer results to NetworkState
    for (node <- hydSim.getnNodes().asScala) {
      val simNode = SimulatedNodes(node.getId())
      simNode.head.computedValue = Meters(node.getSimHead() * 0.3048)
      simNode.demand.computedValue = CubicMetersPerDay(node.getSimDemand() * 28.31685 * 60 * 60 * 24 / 1000)
    }
  }

  /**
   * Simulate the sensors for all computed values
   */
  def simSensors {
    for ((nodeId, node) <- SimulatedNodes) {
      node.head.sensorValue = node.head.sensor.sense(node.head.computedValue)
      node.demand.sensorValue = node.demand.sensor.sense(node.demand.computedValue)
      //node.emitter.sensorValue = node.emitter.sensor.sense(node.emitter.computedValue)
    }
  }

  /**
   * Pretty print to a string the network description
   */
  override def toString() = {
    var str = "\nNetwork:\n"
    for ((nodeId, node) <- SimulatedNodes)
      str = str + "  Node " + nodeId + node.toString() + "\n"
    str
  }
}
