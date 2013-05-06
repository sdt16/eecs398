/*
 * DTCCollection.java
 *
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;
import edu.cwru.eecs398.obdreader.R;

/**
 * 
 * Modified by Schuyler Thompson to work with Android.
 * 
 * Adapted from http://sourceforge.net/projects/elmhandler/
 * 
 * A HashMap based collection of objects of class DTC, keyed on the integer
 * representation of the diagnostic trouble code. The difference between this
 * class and an instance of HashMap is the method provided to poulate the
 * collection from a .csv file.
 * 
 * @author Tim Wootton <tim@tee-jay.demon.co.uk>
 */
public class DTCCollection extends HashMap<Integer, DTC> {

	private static final long serialVersionUID = 1L;

	private static final String TAG = "OBDDTCCollection";

	/** Creates a new instance of DTCCollection */
	public DTCCollection() {
	}

	/**
	 * Creates a new instance of DTCCollection with a specified initial capacity
	 * 
	 * @param initialCapacity
	 *            Value used to set the initial capacity of the unerlying
	 *            HashMap
	 */
	public DTCCollection(final int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Loads this collection with values from the specified file.
	 * <p>
	 * The file is a comma seperated (csv) file, fomatted as one diagnostic
	 * trouble code per line, each line consisting of two comma seperated
	 * fields:
	 * <ol>
	 * <li>string representation of the diagnostic trouble code
	 * <li>description of the diagnostic trouble code
	 * </ol>
	 * <br>
	 * e.g. B1200,Climate Control Pushbutton Circuit Failure
	 * 
	 * @param ctx
	 * 
	 * @param pathname
	 *            The pathname of the file which will be used to populate this
	 *            collection.
	 */
	public void loadFromFile(final Context ctx) {
		String line, code, desc;
		DTC dtc;
		try {
			// final FileInputStream fstream = new FileInputStream(pathname);
			final InputStream fstream = ctx.getResources().openRawResource(
					R.raw.dtc);
			final BufferedReader in = new BufferedReader(new InputStreamReader(
					fstream));
			while (in.ready()) {
				line = in.readLine();
				code = line.substring(0, line.indexOf(','));
				desc = line.substring(line.indexOf(',') + 1);
				dtc = new DTC(code, desc);
				put(dtc.codeAsInt(), dtc);
			}
			in.close();
		} catch (final Exception e) {
			Log.e(TAG, "Error reading the DTC codes from the CSV file", e);
		}
	}

}