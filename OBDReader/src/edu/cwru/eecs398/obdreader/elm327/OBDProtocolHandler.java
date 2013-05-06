/*
 * OBDProtocolHandler.java
 *
 * Copyright (c) 2007-2008 Tim Wootton <tim@tee-jay.demon.co.uk>
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) anyradians_radians_later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.cwru.eecs398.obdreader.elm327;

import java.util.BitSet;

import android.content.Context;
import android.util.Log;

/**
 * Modified by Schuyler Thompson to work with Android.
 * 
 * Adapted from http://sourceforge.net/projects/elmhandler/
 * 
 * @author Tim Wootton <tim@tee-jay.demon.co.uk>
 */
public class OBDProtocolHandler {
	// public BitSet pid_map = null;

	private static final String TAG = "OBDProtocolHandler";

	private static final int POSS_PID_BLKS = 8;
	private static final byte SIZE_PID_BLK = 0x20;
	private static final byte MODE_SHW_CUR = 0x01; // show current data
	private static final byte MODE_SHW_FF = 0x02; // show freezeframe data
	private static final byte MODE_SHW_DTC = 0x03; // show diagnostic trouble
	// codes
	private static final byte MODE_CLR_DTC = 0x04; // clear trouble codes
	private static final byte MODE_TEST_RES_OXY = 0x05; // test results, oxygen
	// sensors
	private static final byte MODE_TEST_RES_NCM = 0x06; // test results,
	// not-continuously-monitored
	// sensors
	private static final byte MODE_SHW_PEN = 0x07; // show pending diagnostic
	// trouble codes
	private static final byte MODE_SPEC_CTRL = 0x08; // special control mode
	private static final byte MODE_REQ_VI = 0x09; // request vehicle information

	private static final byte PID_REQ_SUP_BLK1 = 0x00; // request bitmap of
	// supported parameter
	// IDs 0x01-0x20
	private static final byte PID_REQ_SUP_BLK2 = 0x20; // request bitmap of
	// supported parameter
	// IDs 0x21-0x40
	private static final byte PID_REQ_SUP_BLK3 = 0x40; // request bitmap of
	// supported parameter
	// IDs 0x41-0x60
	private static final byte PID_REQ_DTC_STATUS = 0x01; // request DTC status

	private static final byte PID_FF_DTC = 0x02; // request the DTC that caused
	// the ff data to be stored

	private static final byte PID_REQ_VI_SERIALNO = 0x02;
	private final ELMProtocolHandler elm;

	public DTCCollection dtcCollection;
	public OBDParamCollection liveParameterCollection;
	public OBDParamCollection ffParameterCollection;

	/** Creates a new instance of OBDProtocolHandler */
	public OBDProtocolHandler(final Context ctx, final ELMProtocolHandler elm) {
		this.elm = elm;
		String msg;
		dtcCollection = new DTCCollection(3000);
		dtcCollection.loadFromFile(ctx);
		liveParameterCollection = new OBDParamCollection(300);
		msg = liveParameterCollection.loadFromFile(ctx);
		ffParameterCollection = new OBDParamCollection(300);
		msg = ffParameterCollection.loadFromFile(ctx);
		if (msg != null) {
			Log.w(TAG, msg);
		}
	}

	public boolean clearTroubleCodes() {
		if (elm.sendOBDC(MODE_CLR_DTC) == null) {
			return false;
		} else {
			return true;
		}
	}

	private BitSet getSupportedPIdBlock(final byte pid_sup) {
		String pid_string;
		String[] splitStr;
		final BitSet pid_blk = new BitSet(SIZE_PID_BLK);
		int pid_val = 0;

		pid_string = extractDataFromResponseWithPid(
				elm.sendOBDC(MODE_SHW_CUR, pid_sup), MODE_SHW_CUR, pid_sup);
		pid_string = pid_string.replace("\r\n", "");
		splitStr = pid_string.split(" ");
		for (int i = splitStr.length - 1; i >= 0; i--) {
			pid_val = pid_val
					+ (hexStringToInt(splitStr[i]) << ((splitStr.length - 1 - i) * 8));
		}
		for (int i = 0; i <= (SIZE_PID_BLK - 1); i++) {
			if ((pid_val & 0x01) == 0x01) {
				pid_blk.set(SIZE_PID_BLK - 1 - i, true);
			} else {
				pid_blk.set(SIZE_PID_BLK - 1 - i, false);
			}
			pid_val = pid_val >>> 1;
		}
		return pid_blk;
	}

	public BitSet getSupportedParamIds() {
		final BitSet pid_map = new BitSet(SIZE_PID_BLK * POSS_PID_BLKS);
		BitSet pid_block;
		boolean more_pid_blocks = true;
		byte pid_sup = PID_REQ_SUP_BLK1;

		while (more_pid_blocks) {
			pid_block = getSupportedPIdBlock(pid_sup);
			for (int i = 0; i < SIZE_PID_BLK; i++) {
				pid_map.set(i + pid_sup, pid_block.get(i));
			}
			pid_sup = (byte) (pid_sup + SIZE_PID_BLK);
			more_pid_blocks = pid_block.get(pid_sup - 1);
		}
		return pid_map;
	}

	public DTCandTestData getDTCandTestData() {
		DTCandTestData dat;
		String stored_codes;
		String pending_codes;
		String trouble_code_status;
		int code_ptr;

		trouble_code_status = extractDataFromResponseWithPid(
				elm.sendOBDC(MODE_SHW_CUR, PID_REQ_DTC_STATUS), MODE_SHW_CUR,
				PID_REQ_DTC_STATUS);
		if (trouble_code_status.equals("")) {
			Log.e(TAG, "Trouble code status was empty");
			throw new ErrorMessageException("Trouble code status was empty");
		}
		dat = new DTCandTestData(codeCount(trouble_code_status));
		dat.mil = mil(trouble_code_status);
		dat.freezeFrameCode = doubleHexStringToInt(extractDataFromResponseWithPid(
				elm.sendOBDC(MODE_SHW_FF, PID_FF_DTC), MODE_SHW_FF, PID_FF_DTC));
		pending_codes = extractDataFromResponse(elm.sendOBDC(MODE_SHW_PEN),
				MODE_SHW_PEN);
		stored_codes = extractDataFromResponse(elm.sendOBDC(MODE_SHW_DTC),
				MODE_SHW_DTC);

		if (dat.storedCodes.length != 0) {
			code_ptr = 0;
			while ((!stored_codes.isEmpty())
					&& (code_ptr < dat.storedCodes.length)) {
				dat.storedCodes[code_ptr] = doubleHexStringToInt(stored_codes);
				if (stored_codes.length() <= 6) {
					stored_codes = "";
				} else {
					stored_codes = stored_codes.substring(6);
				}
				code_ptr++;
			}
			code_ptr = 0;
			while ((!pending_codes.isEmpty())
					&& (code_ptr < dat.storedCodes.length)) {
				dat.pendingCodes[code_ptr] = doubleHexStringToInt(pending_codes);
				if (pending_codes.length() <= 6) {
					pending_codes = "";
				} else {
					pending_codes = pending_codes.substring(6);
				}
				code_ptr++;
			}
		}
		dat.misfireTestSupported = testbit(trouble_code_status, 1, 0);
		dat.misfireTestIncomplete = testbit(trouble_code_status, 1, 4);
		dat.fuelSystemTestSupported = testbit(trouble_code_status, 1, 1);
		dat.fuelSystemTestIncomplete = testbit(trouble_code_status, 1, 5);
		dat.componentsTestSupported = testbit(trouble_code_status, 1, 2);
		dat.componentsTestIncomplete = testbit(trouble_code_status, 1, 6);
		dat.reservedTestSupported = testbit(trouble_code_status, 1, 3);
		dat.reservedTestIncomplete = testbit(trouble_code_status, 1, 7);
		dat.catalystTestSupported = testbit(trouble_code_status, 2, 0);
		dat.catalystTestIncomplete = testbit(trouble_code_status, 3, 0);
		dat.heatedCatalystTestSupported = testbit(trouble_code_status, 2, 1);
		dat.heatedCatalystTestIncomplete = testbit(trouble_code_status, 3, 1);
		dat.evaporativeSystemTestSupported = testbit(trouble_code_status, 2, 2);
		dat.evaporativeSystemTestIncomplete = testbit(trouble_code_status, 3, 2);
		dat.secondaryAirSystemTestSupported = testbit(trouble_code_status, 2, 3);
		dat.secondaryAirSystemTestIncomplete = testbit(trouble_code_status, 3,
				3);
		dat.acRefrigerantTestSupported = testbit(trouble_code_status, 2, 4);
		dat.acRefrigerantTestIncomplete = testbit(trouble_code_status, 3, 4);
		dat.o2SensorTestSupported = testbit(trouble_code_status, 2, 5);
		dat.o2SensorTestIncomplete = testbit(trouble_code_status, 3, 5);
		dat.o2SensorHeaterTestSupported = testbit(trouble_code_status, 2, 6);
		dat.o2SensorHeaterTestIncomplete = testbit(trouble_code_status, 3, 6);
		dat.exhaustGasRecircSystemTestSupported = testbit(trouble_code_status,
				2, 7);
		dat.exhaustGasRecircSystemTestIncomplete = testbit(trouble_code_status,
				3, 7);

		return dat;
	}

	public OBDParameter getParam(final byte pid, final byte sub,
			final boolean freezeframe) {
		OBDParameter p;
		final int pkey = (pid << 8) + sub;

		if (freezeframe) {
			p = ffParameterCollection.get(pkey);
		} else {
			p = liveParameterCollection.get(pkey);
		}

		if (p != null) {
			if (freezeframe) {
				p.setValue(extractDataFromResponseWithPid(
						elm.sendOBDC(MODE_SHW_FF, p.pid), MODE_SHW_FF, p.pid));
			} else {
				p.setValue(extractDataFromResponseWithPid(
						elm.sendOBDC(MODE_SHW_CUR, p.pid), MODE_SHW_CUR, p.pid));
			}
		}
		return p;
	}

	public String getVehicleInfo(final byte pid) {
		final String vi = extractDataFromResponseWithPid(
				elm.sendOBDC(MODE_REQ_VI, pid), MODE_REQ_VI, pid);
		return vi;
	}

	private int codeCount(String tcs) {
		tcs = checkForDoubleCode(tcs);
		int count;
		int val = 0;

		if (tcs.length() >= 2) {
			val = hexStringToInt(tcs.substring(0, 2));
		}
		if ((val == 0) && (tcs.length() > 15)) {
			val = hexStringToInt(tcs.substring(12, 14));
		}
		if (val > 128) {
			count = val - 128;
		} else {
			count = val;
		}
		return count;
	}

	private boolean mil(final String tcs) {
		int val;
		if (tcs == null) {
			return false;
		}
		if (tcs.length() < 2) {
			return false;
		}
		val = hexStringToInt(tcs.substring(0, 2));
		if (val > 128) {
			return true;
		} else {
			return false;
		}
	}

	private boolean testbit(final String tcs, final int byteNo, final int bit) {
		int val;
		val = hexStringToInt(tcs.substring(3 * byteNo, (3 * byteNo) + 2));
		if (((val >> bit) & 0x01) == 0x01) {
			return true;
		} else {
			return false;
		}
	}

	private int hexStringToInt(final String str) {
		int retVal = 0;
		if (str == null) {
			return 0;
		}
		if (str.equals("")) {
			return 0;
		}
		try {
			retVal = Integer.parseInt(str, 16);
		} catch (final NumberFormatException e) {
			retVal = 0;
		}
		return retVal;
	}

	private int doubleHexStringToInt(String str) {
		str = checkForDoubleCode(str);
		int idx = 0;
		int val = 0;
		int retVal = 0;
		boolean byte1 = false;
		boolean byte2 = false;

		while (byte2 == false) {
			idx = str.indexOf(' ');
			if (idx == -1) {
				val = hexStringToInt(str.substring(0));
			} else {
				val = hexStringToInt(str.substring(0, idx));
				str = str.substring(idx + 1);
			}
			if (!byte1) {
				retVal = retVal | (val * 0x100);
				byte1 = true;
			} else if (byte1) {
				retVal = retVal | val;
				byte2 = true;
			}
		}
		return retVal;
	}

	private String checkForDoubleCode(final String str) {
		if (str.indexOf("  ") != -1) {
			final String[] split = str.split("  ", 2);
			if (countChars(split[0].toCharArray(), '0') > countChars(
					split[1].toCharArray(), '0')) {
				return split[1];
			} else {
				return split[0];
			}
		}
		return str;
	}

	private int countChars(final char[] chars, final char charToCount) {
		int count = 0;
		for (final char curr : chars) {
			if (curr == charToCount) {
				count++;
			}
		}
		return count;
	}

	private String findCodeStatus(String obdcVal, final String responseId) {
		int idx;
		String tcs;
		if (obdcVal == null) {
			obdcVal = "";
		}
		idx = obdcVal.indexOf(responseId);
		if (idx == 0) {
			tcs = obdcVal.substring(responseId.length() + 1,
					responseId.length() + 3);
		} else {
			tcs = "";
		}
		return tcs;
	}

	private String extractDataFromResponseWithPid(String response,
			final byte mode, final byte pid) {
		int idx;
		String pidPad = "";
		final byte respConv = 0x40;
		String data = "";
		final int moderesp = mode | respConv; // convert into a repsonse mode
		if (pid < 16) {
			pidPad = "0";
		}
		final String responseId = Integer.toHexString(moderesp).toUpperCase()
				+ " " + pidPad + Integer.toHexString(pid).toUpperCase();

		if (response == null) {
			response = "";
		}
		if (response.indexOf(elm.LT) == 0) {
			response = response.substring(elm.LT.length());
		}
		idx = response.indexOf(responseId);
		if (idx < 15) {
			data = response.substring(responseId.length() + 1 + idx);
			while (data.indexOf(elm.LT + responseId) != -1) {
				data = data.replace(elm.LT + responseId, "");
			}
			data = data.replace(elm.LT, "");
		}
		return data;
	}

	private String extractDataFromResponse(String response, final byte mode) {
		int idx;
		final byte respConv = 0x40;
		String data = "";
		final int moderesp = mode | respConv; // convert into a repsonse mode
		final String responseId = Integer.toHexString(moderesp).toUpperCase();

		if (response == null) {
			response = "";
		}
		idx = response.indexOf(responseId);
		if (idx == 0) {
			data = response.substring(responseId.length() + 1);
			while (data.indexOf(elm.LT + responseId) != -1) {
				data = data.replace(elm.LT + responseId, "");
			}
			data = data.replace(elm.LT, "");
		}
		return data;
	}
}