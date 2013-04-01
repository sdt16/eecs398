/*
 * DTC.java
 *
 * Copyright (c) 2007-2008  Tim Wootton <tim@tee-jay.demon.co.uk>
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
 * A class to represent an OBD diagnostic trouble code.
 * 
 * @author Tim Wootton <tim@tee-jay.demon.co.uk>
 */
public class DTC {

	private final int raw_code;
	private final String code;
	private final String description;

	/**
	 * Creates a new instance of DTC.
	 * 
	 * @param code
	 *            The diagnostic trouble code in the form Pxxx, Cxxx, Bxxx or
	 *            Uxxx.
	 * @param desc
	 *            The description of the diagnostic trouble code.
	 */
	public DTC(final String code, final String desc) {
		this.code = code;
		this.description = desc;
		this.raw_code = uncook(code);
	}

	/**
	 * Provides a string description of this diagnostic trouble code.
	 * 
	 * @return The string description of this diagnostic trouble code.
	 */
	public String description() {
		return description;
	}

	/**
	 * Provides a string representation of this diagnostic trouble code.
	 * 
	 * @return The string representation of this diagnostic trouble code in the
	 *         form Pxxx, Cxxx, Bxxx or Uxxx. where the character prefix is
	 *         interpreted as:<br>
	 *         P = Powertrain<br>
	 *         C = Chassis<br>
	 *         B = Body<br>
	 *         U = Network<br>
	 */
	public String codeAsString() {
		return code;
	}

	/**
	 * Provides an integer representation of this diagnostic trouble code.
	 * 
	 * @return The integer representation of this diagnostic trouble code.
	 */
	public int codeAsInt() {
		return raw_code;
	}

	private int uncook(final String code) {
		final char type = code.charAt(0);
		int raw_code = Integer.parseInt(code.substring(1), 16);
		switch (type) {
		case 'P':
		case 'p':
			raw_code = raw_code & 0x3FFF;
			break;
		case 'C':
		case 'c':
			raw_code = (raw_code & 0x3FFF) | 0x4000;
			break;
		case 'B':
		case 'b':
			raw_code = (raw_code & 0x3FFF) | 0x8000;
			break;
		case 'U':
		case 'u':
			raw_code = (raw_code & 0x3FFF) | 0xC000;
			break;
		}
		return raw_code;
	}

	/**
	 * Provides a string representation of the supplied diagnostic trouble code.
	 * 
	 * @param diagCode
	 *            The diagnostic trouble code to be converted to String
	 *            representation.
	 * @return The string representation of the diagnostic trouble code in the
	 *         form Pxxx, Cxxx, Bxxx or Uxxx where the character prefix is
	 *         interpreted as:<br>
	 *         P = Powertrain<br>
	 *         C = Chassis<br>
	 *         B = Body<br>
	 *         U = Network<br>
	 */
	public static String toString(final int diagCode) {
		String cooked;

		cooked = Integer.toHexString((diagCode & 0x3FFF) + 0x10000)
				.toUpperCase();
		switch (diagCode & 0xC000) {
		case 0x0000:
			cooked = cooked.replaceFirst("1", "P");
			break;
		case 0x4000:
			cooked = cooked.replaceFirst("1", "C");
			break;
		case 0x8000:
			cooked = cooked.replaceFirst("1", "B");
			break;
		case 0xC000:
			cooked = cooked.replaceFirst("1", "U");
			break;
		}
		return cooked;
	}
}