package edu.cwru.eecs398.obdreader.elm327;

/**
 * OBDParamCollection.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;
import edu.cwru.eecs398.obdreader.R;

/**
 * 
 * @author Tim Wootton <tim@tee-jay.demon.co.uk>
 * @author Schuyler Thompson, to work with Android.
 */
public class OBDParamCollection extends HashMap<Integer, OBDParameter> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String TAG = "OBDParamCollection";

	public OBDParamFormulaParser parser;

	/** Creates a new instance of DTCCollection */
	public OBDParamCollection() {
		parser = new OBDParamFormulaParser();
	}

	public OBDParamCollection(final int initialCapacity) {
		super(initialCapacity);
		parser = new OBDParamFormulaParser();
	}

	public String loadFromFile(final Context ctx) {

		int count = 0;
		int pkey;
		byte pid = -1;
		int nextPid;
		byte sub = 0;
		String fields[];
		String desc = "", sdesc = "", units = "", formula = "", record;
		int min = 0, max = 0;
		boolean bitmap = false;
		OBDParameter param;

		try {
			final InputStream fstream = ctx.getResources().openRawResource(
					R.raw.pid);
			final BufferedReader in = new BufferedReader(new InputStreamReader(
					fstream));
			record = in.readLine();
			while (in.ready()) {
				count++;
				fields = record.split(",", 7);
				if (fields.length == 7) {
					try {
						pid = Byte.parseByte(fields[0], 16);
					} catch (final NumberFormatException nfe) {
					}
					desc = fields[1];
					sdesc = fields[2];
					try {
						min = Integer.parseInt(fields[3]);
					} catch (final NumberFormatException nfe) {
					}
					try {
						max = Integer.parseInt(fields[4]);
					} catch (final NumberFormatException nfe) {
					}
					units = fields[5];
					formula = fields[6];
					if (formula.equalsIgnoreCase("bit")) {
						bitmap = true;
						formula = "";
					} else {
						bitmap = false;
					}

					pkey = (pid << 8) + sub;

					record = in.readLine();
					try {
						nextPid = Byte.parseByte(
								record.substring(0, record.indexOf(',')), 16);
					} catch (final NumberFormatException nfe1) {
						nextPid = -1;
					}
					if (pid == nextPid) {
						sub++;
						param = new OBDParameter(pid, sub, desc, sdesc, min,
								max, units, bitmap, formula, true, parser);
					} else {
						sub = 0;
						param = new OBDParameter(pid, sub, desc, sdesc, min,
								max, units, bitmap, formula, false, parser);
					}
					put(pkey, param);
				} else {
					return ("File format error, incorrect no. of fields at line " + count);
				}
			}
			in.close();
		} catch (final IOException ioe) {
			Log.e(TAG, "Error reading file", ioe);
			return ("File input error " + ioe.getLocalizedMessage());
		}
		return null;
	}

}