/*
 * DTCandTestData.java
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

/**
 * A class to hold data related to DTCs and Tests. An object of this class will
 * normally be populated by these requests<br>
 * Mode 1 PID 1 - Total Codes, MIL status, Test Support/Complete flags.<br>
 * Mode 2 PID 2 - Freeze Frame Code (the DTC that cause Freezeframe data to be
 * stored.)<br>
 * Mode 3 - Stored DTCs<br>
 * Mode 7 - Pending DTCs
 * 
 * @author Tim Wootton <tim@tee-jay.demon.co.uk>
 */
public class DTCandTestData {
	public int totalCodes;

	/**
	 * DTC that caused freeze frame data to be stored. If 0 then no freeze frame
	 * data has been stored
	 */
	public int freezeFrameCode;
	/**
	 * An array of stored DTCs
	 */
	public int[] storedCodes;
	/**
	 * An array of pending DTCs
	 */
	public int[] pendingCodes;
	/**
	 * Malfunction Indicator Lamp status. If true then MIL is on
	 */
	public boolean mil;
	/**
	 * Flag to indicate if the ECU supports a misfire test.
	 */
	public boolean misfireTestSupported;
	/**
	 * Flag to indicate if the ECU has completed the misfire test.
	 */
	public boolean misfireTestIncomplete;
	/**
	 * Flag to indicate if the ECU supports a fuel system test.
	 */
	public boolean fuelSystemTestSupported;
	/**
	 * Flag to indicate if the ECU has completed the fuel system test.
	 */
	public boolean fuelSystemTestIncomplete;
	/**
	 * Flag to indicate if the ECU supports a components test. //TODO What is a
	 * components test
	 */
	public boolean componentsTestSupported;
	/**
	 * Flag to indicate if the ECU has completed the components test.
	 */
	public boolean componentsTestIncomplete;
	/**
	 * Reserved by the OBD standards, should always be false.
	 */
	public boolean reservedTestSupported;
	/**
	 * Reserved by the OBD standards, should always be false.
	 */
	public boolean reservedTestIncomplete;
	/**
	 * Flag to indicate if the ECU supports a catalyst test.
	 */
	public boolean catalystTestSupported;
	/**
	 * Flag to indicate if the ECU has completed the catalyst test.
	 */
	public boolean catalystTestIncomplete;
	/**
	 * Flag to indicate if the ECU supports a heated catalyst test.
	 */
	public boolean heatedCatalystTestSupported;
	/**
	 * Flag to indicate if the ECU has completed the heated cayalyst test.
	 */
	public boolean heatedCatalystTestIncomplete;
	/**
	 * Flag to indicate if the ECU supports a evaposative system.
	 */
	public boolean evaporativeSystemTestSupported;
	/**
	 * Flag to indicate if the ECU has completed the evaporative system test.
	 */
	public boolean evaporativeSystemTestIncomplete;
	/**
	 * Flag to indicate if the ECU supports a secondary air system test.
	 */
	public boolean secondaryAirSystemTestSupported;
	/**
	 * Flag to indicate if the ECU has completed the secondary air system test.
	 */
	public boolean secondaryAirSystemTestIncomplete;
	/**
	 * Flag to indicate if the ECU supports an air conditioning refrigerant
	 * test.
	 */
	public boolean acRefrigerantTestSupported;
	/**
	 * Flag to indicate if the ECU has completed the air conditioning
	 * refrigerant test.
	 */
	public boolean acRefrigerantTestIncomplete;
	/**
	 * Flag to indicate if the ECU supports an O2 sensor test
	 */
	public boolean o2SensorTestSupported;
	/**
	 * Flag to indicate if the ECU has completed the O2 sensor test.
	 */
	public boolean o2SensorTestIncomplete;
	/**
	 * Flag to indicate if the ECU supports an O2 sensor heater test
	 */
	public boolean o2SensorHeaterTestSupported;
	/**
	 * Flag to indicate if the ECU has completed the O2 sensor heater test.
	 */
	public boolean o2SensorHeaterTestIncomplete;
	/**
	 * Flag to indicate if the ECU supports an exhaust gas recirculation test
	 */
	public boolean exhaustGasRecircSystemTestSupported;
	/**
	 * Flag to indicate if the ECU has completed the exhaust gas recirculation
	 * test.
	 */
	public boolean exhaustGasRecircSystemTestIncomplete;

	/**
	 * Creates a new instance of DTCandTestData
	 * 
	 * @param maxCodes
	 *            sets the number of DTCs of <b>each</b> type (stored & pending)
	 *            that can be accomodated
	 */
	public DTCandTestData(final int maxCodes) {
		totalCodes = maxCodes;
		storedCodes = new int[totalCodes];
		pendingCodes = new int[totalCodes];
	}

}