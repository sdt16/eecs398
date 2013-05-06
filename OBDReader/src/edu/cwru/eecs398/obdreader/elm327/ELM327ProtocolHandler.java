package edu.cwru.eecs398.obdreader.elm327;

import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

/**
 * Modified by Schuyler Thompson to work with Android.
 * 
 * Adapted from http://sourceforge.net/projects/elmhandler/
 * 
 * Copyright (c) 2007-2008 Tim Wootton <tim@tee-jay.demon.co.uk>
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option)
 * anyradians_radians_later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * References: ELM327DSA OBD to RS232 Interpreter data Sheet by Elm Electronics
 * <www.emlelectronics.com>
 */
public class ELM327ProtocolHandler extends ELMProtocolHandler {

	private static final String TAG = "ELM327ProtocolHandler";

	public enum OBDProtocol {
		OBD_AUTO, OBD_SAE_J1850_PWM, OBD_SAE_J1850_VPW, OBD_ISO_9141_2, OBD_ISO_14230_4_KWP, OBD_ISO_14230_4_KWP_FAST_INIT, OBD_ISO_15765_4_CAN_11BIT_500KB, OBD_ISO_15765_4_CAN_29BIT_500KB, OBD_ISO_15765_4_CAN_11BIT_250KB, OBD_ISO_15765_4_CAN_29BIT_250KB
	}

	public enum ISOBaudRate {
		ISO_BAUD_10400, ISO_BAUD_9600
	}

	public ELM327ProtocolHandler(final InputStream in, final OutputStream out) {
		super(in, out);
		// TODO Auto-generated constructor stub
	}

	/*
	 * As described in the ELM documentation AT Command summary OBD Commands
	 */

	/**
	 * OBD messages are normally restricted to 7 bytes both tx & rx, this method
	 * enables or disables long messages of 8 bytes tx, and 'unlimited' rx.
	 * 
	 * @param longmessage
	 *            true to enable long messgages, false to disable them.
	 * @return true if the enable or disable was sucessful.
	 */
	public synchronized boolean setLongMsg(final boolean longmessage) {
		if (longmessage) {
			Log.d(TAG, "sending allow long msg cmd atal");
			sendAt("al");
		} else {
			Log.d(TAG, "sending disallow long msg cmd atnl");
			sendAt("nl");
		}
		return waitForOk();
	}

	/**
	 * The ELM chip contains an internal 12 byte OBD message buffer, which
	 * contains the last data sent or recieved, this method dumps the contents
	 * of the buffer.
	 * 
	 * @return A string representing the 12 data bytes, preceded by a length
	 *         which is the total length of the data that was recieved even if
	 *         not all of the data fits in the 12 byte buffer.
	 */
	public synchronized String bufferDump() {
		Log.d(TAG, "requesting buffer dump with cmd atbd");
		sendAt("bd");
		return waitForString(OK + LT + LT + PROMPT);
	}

	/**
	 * <B><U>**ELM's documentation advises that this command should be used with
	 * caution.**</U></B> <br>
	 * Allow an OBD protocol to be made active without any initiation or
	 * handshaking. ELM say it is only provided to allow the construction of ECU
	 * simulators and training demonstrators.
	 * <p>
	 * You have been warned!
	 * 
	 * @return true if "Bypass Init" mode was sucessfully enabled.
	 */
	public synchronized boolean setBypassInit() {
		Log.d(TAG, "sending bypass init cmd atbi");
		sendAt("bi");
		return waitForOk();
	}

	/**
	 * Fetch the text description of the active OBD protocol.
	 * 
	 * @return The text description of the OBD protocol, preceded by the word
	 *         AUTO if the protocol was selected by automatic protocol
	 *         detection.
	 */
	public synchronized String protocolDesc() {
		Log.d(TAG, "sending protocol description cmd atdp");
		sendAt("dp");
		return waitForString(LT + PROMPT);
	}

	/**
	 * Fetch the protocol number of the active OBD protocol.
	 * 
	 * @return a string representing the protocol number, preceded by the
	 *         character 'A' if the protocol was selected by automatic protocol
	 *         detection.
	 *         <p>
	 * 
	 *         1 - SAE J1850 PWM (41.6 Kbaud)<br>
	 *         2 - SAE J1850 VPW (10.4 Kbaud)<br>
	 *         3 - ISO 9141-2 (5 baud init, 10.4 Kbaud)<br>
	 *         4 - ISO 14230-4 KWP (5 baud init, 10.4 Kbaud)<br>
	 *         5 - ISO 14230-4 KWP (fast init, 10.4 Kbaud)<br>
	 *         6 - ISO 15765-4 CAN (11 bit ID, 500 Kbaud)<br>
	 *         7 - ISO 15765-4 CAN (29 bit ID, 500 Kbaud)<br>
	 *         8 - ISO 15765-4 CAN (11 bit ID, 250 Kbaud)<br>
	 *         9 - ISO 15765-4 CAN (29 bit ID, 250 Kbaud)<br>
	 */
	public synchronized String protocolNumber() {
		Log.d(TAG, "sending protocol number cmd atdn");
		sendAt("dpn");
		return waitForString(LT + PROMPT);
	}

	/**
	 * Enable or disable header bytes in OBD responses. Headers are disabled by
	 * default. You will actually get more than just the header, the complete
	 * message including the check-digits, and PCI bytes are provided. The
	 * exception is that ELM chip does not currently respond with the CAN data
	 * length code (DLC), the CRC, or the special J1850 IFR bytes.
	 * 
	 * @param headers
	 *            true to enable headers in responses, false to disable. By
	 *            default the headers are off.
	 * @return true if the headers were sucessfully enabled or disabled
	 */
	public synchronized boolean setHeaders(final boolean headers) {
		if (headers) {
			Log.d(TAG, "sending enable headers cmd ath1");
			sendAt("h1");
		} else {
			Log.d(TAG, "sending disable headers cmd ath0");
			sendAt("h0");
		}
		return waitForOk();
	}

	/**
	 * Enable or disable storage of last OBD protocol used to the ELM327 chip's
	 * non-volatile memory. Default on power up is determined by logic level on
	 * pin 5 of the chip (high=on low=off), this method overrides that setting.
	 * The stored OBD protocol will be the first attempted at next power on.
	 * When the memory function is enabled, each time the ELM327 finds a valid
	 * OBD protocol, that protocol is stored and becomes the new default. If the
	 * memory function is not enabled, any protocols found during a session will
	 * not be memorized, and the ELM327 will always revert at power up to using
	 * the same (last saved) protocol.
	 * 
	 * @param protocolmemory
	 *            true to enable protocol memory function, false to disable.
	 * @return true if protocol memory was sucessfully enabled/disabled.
	 */
	public synchronized boolean setProtocolMemory(final boolean protocolmemory) {
		if (protocolmemory) {
			Log.d(TAG, "sending enable protocol memory cmd atm1");
			sendAt("m1");
		} else {
			Log.d(TAG, "sending disable protocol memoty cmd atm0");
			sendAt("m0");
		}
		return waitForOk();
	}

	/**
	 * Not implemented
	 * 
	 * @return Not implemented
	 */
	public synchronized String monitorAll() {
		// TODO Monitor all MA
		return "not yet implemented";
	}

	/**
	 * Not implemented
	 * 
	 * @param header
	 *            Not implemented
	 * @return Not implemented
	 */
	public synchronized String monitorRx(final String header) {
		// TODO Monitor for reciever MR
		return "not yet implemented";
	}

	/**
	 * Not implemented
	 * 
	 * @param header
	 *            Not implemented
	 * @return Not implemented
	 */
	public synchronized String monitorTx(final String header) {
		// TODO Monitor for Transmitter MT
		return "not yet implemented";
	}

	// TODO Protocol Close

	/**
	 * <B><U>**ELM's documentation advises that this command should not normally
	 * be used.**</U></B> <br>
	 * Allow the ELM chip to send OBD requests blindly without waiting for a
	 * response. This may cause the vehicle difficulty if it's waiting for an
	 * acknowledgement that it won't ever recieve. Default is reponses on. This
	 * method should not normally be called if communicating with a vehicle,
	 * however it could be used when using the chip for other applications, or
	 * in ECU simulators.
	 * <p>
	 * You have been warned!
	 * 
	 * @param resp
	 *            true to enable responses, false to disable.
	 * @return true if the resonses were sucessfully enabled/disabled.
	 */
	public synchronized boolean setResponses(final boolean resp) {
		if (resp) {
			Log.d(TAG, "sending enable responses cmd atr1");
			sendAt("r1");
		} else {
			Log.d(TAG, "sending disable responses cmd atr0");
			sendAt("r0");
		}
		return waitForOk();
	}

	// TODO Set Header SH
	// TODO Set Protocol SP
	/*
	 * 0 - Automatic 1 - SAE J1850 PWM (41.6 Kbaud) 2 - SAE J1850 VPW (10.4
	 * Kbaud) 3 - ISO 9141-2 (5 baud init, 10.4 Kbaud) 4 - ISO 14230-4 KWP (5
	 * baud init, 10.4 Kbaud) 5 - ISO 14230-4 KWP (fast init, 10.4 Kbaud) 6 -
	 * ISO 15765-4 CAN (11 bit ID, 500 Kbaud) 7 - ISO 15765-4 CAN (29 bit ID,
	 * 500 Kbaud) 8 - ISO 15765-4 CAN (11 bit ID, 250 Kbaud) 9 - ISO 15765-4 CAN
	 * (29 bit ID, 250 Kbaud)
	 */
	// TODO Set timeout ST
	// TODO Try protocol TP

	/*
	 * As described in the ELM documentation AT Command summary CAN Specific
	 * Commands
	 */

	/**
	 * Enable or disable CAN auto formatting. With CAN auto formatting enabled,
	 * the ELM327 will generate the formatting (PCI) bytes for your requests
	 * automatically. It will also remove them when recieving along with any
	 * trailing unused bytes. This means that you can still send OBD requests
	 * (like 01 04) without regard to the extra bytes CAN systems require.<br>
	 * Default is CAN auto formatting on.<br>
	 * Setting headers on with {@link #setHeaders(boolean) setHeaders()}, will
	 * turn CAN auto formatting off.
	 * 
	 * @param autoformat
	 *            true to enable CAN auto format, false to disable.
	 * @return true if CAN auto format sucessfully enabled/disabled.
	 */
	public synchronized boolean setCANAutoFormat(final boolean autoformat) {
		if (autoformat) {
			sendAt("caf1");
		} else {
			sendAt("caf0");
		}
		return waitForOk();
	}

	/*
	 * public synchronized boolean setCAN_11BitFilter(int filter){ //TODO //
	 * Integer i.pa if((filter >= 0)&(filter <= 0x7FF)){ // sendAt("cf " +
	 * sFilter.); return waitForOk(); } else{
	 * logger.finest("CAN 11 bit filter value " + filter + " out of range");
	 * return false; } }
	 */

	/*
	 * public synchronized boolean setCAN_29BitFilter(int filter){ //TODO String
	 * sFilter = ""; if((filter >= 0)&(filter <= 0x1FFFFFFF)){ sendAt("cf" +
	 * sFilter); return waitForOk(); } else{
	 * logger.finest("CAN 29 bit filter value " + filter + " out of range");
	 * return false; } }
	 */

	/**
	 * Enable or disable ISO 15765-4 flow control messages. ISO 15765-4 protocol
	 * expects a flow control message to be sent in response to a "First Frame"
	 * message. The ELM327 automatically sends these, without intervention. In a
	 * non-OBD system, it may be desirable to disable this automatic response.<br>
	 * The default setting is flow control messages on.<br>
	 * <B>Note:</B> during CAN monitoring {@link #monitorAll() monitorAll() },
	 * {@link #monitorTx(String) monitorTx() },or {@link #monitorRx(String)
	 * monitorRx() }, no flow control messages are sent regardless of this
	 * setting.
	 * 
	 * @param flow
	 *            true to enable flow contol messages, false to disable.
	 * @return true if flow control messages were sucesfully enabled/disabled.
	 */
	public synchronized boolean setCANFlowControl(final boolean flow) {
		if (flow) {
			sendAt("cfc1");
		} else {
			sendAt("cfc0");
		}
		return waitForOk();
	}

	/*
	 * public synchronized boolean setCAN_11BitIdMask(int mask){ //TODO String
	 * sMask = ""; if((mask >= 0)&(mask <= 0x7FF)){ sendAt("cm" + sMask); return
	 * waitForOk(); } else{ logger.finest("CAN 11 bit ID mask value " + mask +
	 * " out of range"); return false; } }
	 */

	/*
	 * public synchronized boolean setCAN_29BitIdMask(int mask){ //TODO String
	 * sMask = ""; if((mask >= 0)&(mask <= 0x1FFFFFFF)){ sendAt("cm" + sMask);
	 * return waitForOk(); } else{ logger.finest("CAN 29 bit ID mask value " +
	 * mask + " out of range"); return false; } }
	 */

	/*
	 * public synchronized boolean setCanPriority(byte pri){ if((mask >= 0)
	 * &(mask <= 0x1F)){ //TODO } else{ logger.finest("CAN priority value " +
	 * pri + " out of range"); return false; // } }
	 */

	/**
	 * Fetch the CAN bus statistics.
	 * 
	 * @return Tx and Rx error statistics for the CAN bus. If the Tx error count
	 *         is so high that the transmitter has been disabled ( > FF) the Tx
	 *         error count will show 'OFF'
	 */
	public synchronized String CANStats() {
		sendAt("cs");
		return waitForString(OK + LT + LT + PROMPT);
	}

	/*
	 * As described in the ELM documentation AT Command summary ISO Specific
	 * Commands
	 */

	/**
	 * Sets the baud rate used for the ISO 9141-2 and ISO 14230-4 protocols.
	 * (protocol numbers 3, 4, and 5)
	 * 
	 * @param rate
	 *            valid values are ISO_BAUD_10400 or ISO_BAUD_9600.
	 * @return true if baud rate was sucessfully set to the requested rate.
	 */
	public synchronized boolean setISOBaudRate(final ISOBaudRate rate) {
		if (rate == ISOBaudRate.ISO_BAUD_10400) {
			return sendAt("ib10");
		}
		if (rate == ISOBaudRate.ISO_BAUD_9600) {
			return sendAt("ib96");
		}
		return false;
	}

	// TODO Wakeup Message SW
	// TODO Wakeup Message WM

	/*
	 * As described in the ELM documentation AT Command summary Misc. Commands
	 */

	/**
	 * Measure battery voltage. This method measures the voltage on Pin 2 of the
	 * ELM327 chip. The example application in the Data Sheet shows this via a
	 * 47K and 10K resistor divider from Pin 16 of the J1962 Vehicle Connector
	 * (and this will be the case in most real application circuits). This gives
	 * us the car battery voltage, and is the reference voltage for all the
	 * sensor readings. <B>Note</B> while this will normally be +12v, it could
	 * also be +6v or +24v, values up to about 28v can be measured in this way.
	 * Uncalibrated accuracy is expected to be about 2%.
	 * 
	 * @return Battery Voltage
	 */
	public synchronized float voltage() {
		int idx;
		sendAt("rv");
		String volts_string = waitForString(LT + PROMPT);
		idx = volts_string.indexOf('V');
		if (idx == -1) {
			return -1;
		}
		volts_string = volts_string.substring(0, volts_string.indexOf('V'));
		return Float.parseFloat(volts_string);
	}

	/**
	 * Calibrate the reading given by {@link #voltage() voltage() }. To use this
	 * calibration take a voltmeter with sufficient accuracy and read the actual
	 * input voltage between pins 5 and 16 of the J1962 Vehicle Connector and
	 * use this value (up to 2 decimal places are significant) as the 'volts'
	 * parameter to this method.
	 * 
	 * @param volts
	 *            The measured voltage with which to calibrate (up to 2 decimal
	 *            places are significant).
	 * @return true if the calibration was sucessfull.
	 */
	public synchronized boolean calibrateVoltage(final float volts) {
		final int v = (int) volts / 1;
		final int w = (int) volts % 1;
		Log.d(TAG, "Voltage calibrating to " + v + "." + w + "v\n");
		final String volts_string = pad(v) + pad(w);
		sendAt("cv " + volts_string);
		return waitForOk();
	}

}
