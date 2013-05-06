package edu.cwru.eecs398.obdreader.elm327;

/*
 * OBDParameter.java
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

/**
 * * Modified by Schuyler Thompson to work with Android.
 * 
 * Adapted from http://sourceforge.net/projects/elmhandler/
 * 
 * A class to represent and calculate values for an OBD parameter
 * 
 * @author Tim Wootton <tim@tee-jay.demon.co.uk>
 */
public class OBDParameter {

	/**
	 * Parameter identifier of the OBD paramater.
	 */
	public byte pid;
	/**
	 * Parameter sub pid of the OBD paramater.
	 */
	public byte sub;
	/**
	 * Full description of the OBD parameter
	 */
	public String description;
	/**
	 * Brief and/or abreviated description of the OBD parameter
	 */
	public String short_desc;
	/**
	 * Minimun permitted value for this OBD parameter.
	 */
	public double min;
	/**
	 * Maximum permitted value for this OBD parameter.
	 */
	public double max;
	/**
	 * Description of the units in which this OBD parameter is expressed.
	 */
	public String units;
	/**
	 * Flag to indicate if this OBD parameter represents a bit-map rather than a
	 * value.
	 */
	public boolean bitmap;
	/**
	 * Representation of the formula, in a suitable format for use by JEP, that
	 * will convert the raw data from OBD into the actual value.
	 */
	public String formula;

	private String raw;
	private double value;
	private final boolean more;

	/**
	 * Creates a new instance of OBDParameter
	 * 
	 * @param pid
	 *            parameter identifier of the OBD parameter.
	 * @param sub
	 *            parameter sub-pid for where more than 1 value is derived from
	 *            a pid.
	 * @param description
	 *            full description of the parameter.
	 * @param short_desc
	 *            brief descroption of the OBD parameter.
	 * @param min
	 *            Minimum permissable calculated value of the OBD parameter.
	 * @param max
	 *            Maximum permissable calculated value of the OBD parameter.
	 * @param units
	 *            Description of the units in which the calculated OBD parameter
	 *            value is expressed.
	 * @param bitmap
	 *            Flag to indicate if this parameter represents a bitmap rather
	 *            than a calculated value.
	 * @param formula
	 */
	public OBDParameter(final byte pid, final byte seq,
			final String description, final String short_desc, final int min,
			final int max, final String units, final boolean bitmap,
			final String formula, final boolean more,
			final OBDParamFormulaParser parser) {
		this.pid = pid;
		this.sub = seq;
		this.description = description;
		this.short_desc = short_desc;
		this.min = min;
		this.max = max;
		this.units = units;
		this.bitmap = bitmap;
		this.formula = formula;
		this.raw = "";
		this.value = 0;
		this.more = more;
	}

	/**
	 * 
	 * @param rawValue
	 * @return cooked value
	 */
	private double cook(String rawValue) {
		boolean moreBytes = true;
		int i = 0;
		String aHexByte;
		final int[] v = { 0, 0, 0, 0, 0, 0 };

		while (moreBytes) {
			if (rawValue.indexOf(' ') != -1) {
				aHexByte = rawValue.substring(0, rawValue.indexOf(' '));
				rawValue = rawValue.substring(rawValue.indexOf(' ') + 1);
			} else {
				aHexByte = rawValue.substring(0);
				moreBytes = false;
			}
			try {
				v[i] = Integer.parseInt(aHexByte, 16);
			} catch (final NumberFormatException nfe) {
				moreBytes = false;
			}
			i++;
		}
		return OBDParamFormulaParser.eval(formula, v[0], v[1], v[2], v[3],
				v[4], v[5]);
	}

	/**
	 * Sets the value of this OBD parameter.
	 * 
	 * @param rawValue
	 *            A string representation of the raw bytes that represent the
	 *            value of the parameter as returned by an OBD device (with the
	 *            reponse codes removed), up to 6 bytes as space seperated
	 *            hexadecimal pairs, e.g. 00 00 01 4F 2C 02
	 * @return The value of the OBD parameter calculated by applying the formula
	 *         to the raw data provided.
	 */
	public double setValue(final String rawValue) {
		raw = rawValue;
		value = cook(rawValue);
		// TODO check against min/max?
		return value;
	}

	/**
	 * Gets the value of the OBD parameter calculated from the last data
	 * provided by setValue()
	 * 
	 * @return the value of the OBD parameter
	 */
	public double value() {
		return value;
	}

	/**
	 * Gets the raw data string as previously set by setValue()
	 * 
	 * @return the raw data string representation of the OBD parameter
	 */
	public String rawValue() {
		return raw;
	}

	/**
	 * OBD parameters sometimes represent more than one value, where this occurs
	 * multiple OBDParameters objects will be created, each will contain the
	 * same raw data, but will have a different formula and therefore different
	 * values. This method determines if this object represents the last value
	 * represented by the OBD parameter.
	 * 
	 * @return a flag which shows if this is the last value represented by this
	 *         OBD parameter
	 */
	public boolean hasMore() {
		return more;
	}
}