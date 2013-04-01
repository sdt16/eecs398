package edu.cwru.eecs398.obdreader.elm327;

import org.nfunk.jep.JEP;

/*
 * OBDParamFormulaParser.java
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
 * 
 * @author Tim Wootton <tim@tee-jay.demon.co.uk>
 */
public class OBDParamFormulaParser {

	static public double eval(final String formula, final int a, final int b,
			final int c, final int d, final int e, final int f) {
		final JEP j = new JEP();
		j.addVariable("A", a);
		j.addVariable("B", b);
		j.addVariable("C", c);
		j.addVariable("D", d);
		j.addVariable("E", e);
		j.addVariable("F", f);
		j.parseExpression(formula);
		return j.getValue();
	}
}
