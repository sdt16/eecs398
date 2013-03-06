package edu.cwru.eecs398.obdreader.elm327;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ByteToObdPrefix {
	
	public static final Map<Character, String> BYTE_TO_PREFIX;
    static {
        Map<Character, String> aMap = new HashMap<Character, String>();
        aMap.put('0', "P0");
        aMap.put('1', "P1");
        aMap.put('2', "P2");
        aMap.put('3', "P3");
        aMap.put('4', "C0");
        aMap.put('5', "C1");
        aMap.put('6', "C2");
        aMap.put('7', "C3");
        aMap.put('8', "B0");
        aMap.put('9', "B1");
        aMap.put('A', "B2");
        aMap.put('B', "B3");
        aMap.put('C', "U0");
        aMap.put('D', "U1");
        aMap.put('E', "U2");
        aMap.put('F', "U3");
        BYTE_TO_PREFIX = Collections.unmodifiableMap(aMap);
    }
	
	
	/**
	 * No need to ever construct this class. Used purely as a place to keep the above map.
	 */
	private ByteToObdPrefix() {
		super();
	}

}
