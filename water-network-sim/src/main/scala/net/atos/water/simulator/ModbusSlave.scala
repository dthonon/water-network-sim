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
import net.wimpi.modbus.procimg.SimpleInputRegister
import net.wimpi.modbus.Modbus

import org.slf4j.LoggerFactory
import ch.qos.logback.core.util.StatusPrinter
import ch.qos.logback.classic.LoggerContext

import org.apache.poi.hssf.usermodel.HSSFHeader;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel._;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel._;
import org.apache.poi.hssf.usermodel.HeaderFooter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Modbus slave methods:
 * - initialize: Creates the modbus interface
 * - shutdown: Ends modbus interface
 * - createRegisters: Creates modbus map
 * - updateRegisters: Copies sensor values to modbus map
 */
class ModbusSlave {
  def logger = LoggerFactory.getLogger(this.getClass().getName())
  var spi: SimpleProcessImage = new SimpleProcessImage()
  var listener: ModbusTCPListener = null

  /**
   * Initialize modbus TCP/IP interface
   *
   * @param port TCP/IP port for listeners
   */
  def initialize(port: Int = Modbus.DEFAULT_PORT) {
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

  /**
   * Stop modbus interface and listeners (release TCP/IP port)
   */
  def shutdown {
    listener.stop();
  }

  /**
   * Create the modbus map (one big map for the moment):
   *  - create registers for all network elements: nodes, links
   *  - if required, output this map to an Excel spreadsheet
   *
   * @param netw
   * @param modbusMap
   */
  def createRegisters(netw: NetworkDescription, modbusMap: String = "") {
    var reg = 0 // register number
    var mapOut = false // true only if map output required
    val wb: XSSFWorkbook = new XSSFWorkbook // Create Excel workbook
    val sheet: Sheet = wb.createSheet("Modbus map"); // Create Excel sheet

    if (!modbusMap.isEmpty()) {
      // Required modbusMap export, check name
      if (modbusMap.endsWith(".xlsx")) {
        logger.info("Creating modbus map file: " + modbusMap)
        mapOut = true
        
        // Create header row
        val row = sheet.createRow(0)
        row.createCell(0).setCellValue("Register")
        row.createCell(1).setCellValue("Network structure")
        row.createCell(2).setCellValue("Value type")
        row.createCell(3).setCellValue("Element ID")
        row.createCell(4).setCellValue("Scale")
      } else {
        logger.warn("Incorrect modbusMap filename (should end with .xlsx" + modbusMap)
      }
    }
    // Registers for node output
    for ((nodeId, node) <- netw.SimulatedNodes) {
      // Create 2 registers in the modbus map
      logger.debug("Modbus register " + reg + " node head -> " + nodeId)
      if (mapOut) { // Add Excel row
        val row = sheet.createRow(reg + 1)
        row.createCell(0).setCellValue(reg)
        row.createCell(1).setCellValue(node.elementType.toString())
        row.createCell(2).setCellValue(node.head.name)
        row.createCell(3).setCellValue(nodeId.getNode().getId())
        row.createCell(4).setCellValue(1)
      }
      spi.addRegister(new SimpleInputRegister(0))
      reg += 1
      logger.debug("Modbus register " + reg + " node demand -> " + nodeId)
      if (mapOut) { // Add Excel row
        val row = sheet.createRow(reg + 1)
        row.createCell(0).setCellValue(reg)
        row.createCell(1).setCellValue(node.elementType.toString())
        row.createCell(2).setCellValue(node.demand.name)
        row.createCell(3).setCellValue(nodeId.getNode().getId())
        row.createCell(4).setCellValue(100)
      }
      spi.addRegister(new SimpleRegister(0))
      reg += 1
    }

    // Registers for link values
    for ((linkId, link) <- netw.SimulatedLinks) {
      // Create 2 registers in the modbus map
      logger.debug("Modbus register " + reg + " link status -> " + linkId)
      if (mapOut) { // Add Excel row
        val row = sheet.createRow(reg + 1)
        row.createCell(0).setCellValue(reg)
        row.createCell(1).setCellValue(link.elementType.toString())
        row.createCell(2).setCellValue(link.status.name)
        row.createCell(3).setCellValue(linkId.getLink().getId())
        row.createCell(4).setCellValue(0)
      }
      spi.addRegister(new SimpleRegister(0))
      reg += 1
      logger.debug("Modbus register " + reg + " link flow -> " + linkId)
      if (mapOut) { // Add Excel row
        val row = sheet.createRow(reg + 1)
        row.createCell(0).setCellValue(reg)
        row.createCell(1).setCellValue(link.elementType.toString())
        row.createCell(2).setCellValue(link.flow.name)
        row.createCell(3).setCellValue(linkId.getLink().getId())
        row.createCell(4).setCellValue(100)
      }
      spi.addRegister(new SimpleInputRegister(0))
      reg += 1
    }
    if (mapOut) {
      val fileOut = new FileOutputStream(modbusMap)
      wb.write(fileOut);
      fileOut.close();

    }
  }

  /**
   * Copy sensors values to the modbus map
   *
   * @param netw Network containing sensor data
   */
  def updateRegisters(netw: NetworkDescription) {
    var reg = 0 // Register number

    // Node values
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

    // Link values
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
    
  /**
   * Read modbus map to detect changes due to master requestss
   *
   * @param netw Network containing sensor data
   */
  def readRegisters(netw: NetworkDescription) {
    var reg = 0 // Register number

    // Node values
    for ((nodeId, node) <- netw.SimulatedNodes) {
      val dem = spi.getRegister(reg).getValue()
      logger.trace("Getting register " + reg + " = " + dem)
      reg += 1
      val head = spi.getRegister(reg).getValue()
      logger.trace("Getting register " + reg + " = " + head)
      reg += 1
    }

    // Link values
    for ((linkId, link) <- netw.SimulatedLinks) {
      val status = spi.getRegister(reg).getValue()
      logger.trace("Getting register " + reg + " = " + status)
      reg += 1
      if (link.status.sensorValue.id != status) {
        logger.debug("Link " + linkId + " status change to " + status)
        link.status = LinkStatus(status)
      }
      val flow = spi.getRegister(reg).getValue()
      logger.trace("Getting register " + reg + " = " + flow)
      reg += 1
    }
  }

}