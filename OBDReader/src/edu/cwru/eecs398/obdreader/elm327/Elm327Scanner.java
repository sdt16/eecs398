package edu.cwru.eecs398.obdreader.elm327;

import static edu.cwru.eecs398.obdreader.elm327.Elm327Commands.GET_NUMBER_OF_CODES;
import static edu.cwru.eecs398.obdreader.elm327.Elm327Commands.GET_CODES;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class Elm327Scanner implements Scanner{
	
	private static final String TAG = "OBD";
	
	private final BluetoothSocket socket;
	
	private final InputStream iS;
	private final OutputStream oS;
	
	public Elm327Scanner(BluetoothSocket btSocket) {
		this.socket = btSocket;
		InputStream tempIS = null;
		OutputStream tempOS = null;
		try {
			tempIS = socket.getInputStream();
			tempOS = socket.getOutputStream();
		} catch (IOException e) {
			Log.e("OBD", "Error getting bluetooth stream", e);
		}
		
		this.iS = tempIS;
		this.oS = tempOS;
		
	}

	public List<String> scan() {
		String[] rawData = getNumberOfCodesRawString();
		int numCodes = processNumberOfCodes(rawData);
		boolean milOn = milOn(rawData);
		
		List<String> codes = readCodes(numCodes);
		
		return codes;
	}
	
	private List<String> readCodes(int numCodes) {
		sendData(GET_CODES.getCommand());
		String[] rawData = splitLine(receiveLine());
		List<String> codes = new ArrayList<String>();
		for (int i = 1; i<=numCodes*2; i+=2) {
			codes.add(interpretRawObdCode(rawData[i] + rawData[i+1]));
		}
		return codes;
	}

	private String interpretRawObdCode(String string) {
		StringBuilder sb = new StringBuilder();
		sb.append(ByteToObdPrefix.BYTE_TO_PREFIX.get(string.charAt(0)));
		sb.append(string.substring(1));
		return sb.toString();
	}

	private String[] getNumberOfCodesRawString() {
		sendData(GET_NUMBER_OF_CODES.getCommand());
		String returnData = receiveLine();
		return splitLine(returnData);
	}
	
	private int processNumberOfCodes(String[] splitCodes) {
		if (!splitCodes[0].equals("41") || !splitCodes[1].equals("01")) {
			throw new CommunicationException("Error from ELM, response code not correct.");
		}
		int rawData = Integer.valueOf(splitCodes[2], 16);
		return rawData - 128;
	}
	
	private boolean milOn(String[] splitCodes) {
		int rawData = Integer.valueOf(splitCodes[2], 16);
		return (rawData > 127);
	}
	
	private String[] splitLine(String returnData) {
		return returnData.split(" ");
	}

	private String receiveLine() {
		int read = Integer.MAX_VALUE;
		String readString = null;
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			while (read != '\r') {
				read = iS.read();
				byteStream.write(read);
				//Log.d("OBD", "Value read from socket is: " + read);
				//break;
			}
			readString = new String(byteStream.toByteArray(), "US-ASCII");
			Log.d(TAG, "Value read from socket is: " + readString);			
		} catch (IOException e) {
			Log.e(TAG, "Error reading from input stream", e);
		}
		
		return readString;
	}

	private void sendData(String data) {
		try {
			oS.write(data.getBytes("US-ASCII"));
		} catch (UnsupportedEncodingException e) {
			throw new CommunicationException("Unsupported Encoding", e);
		} catch (IOException e) {
			throw new CommunicationException("Problem writing to ELM327", e);
		}
	}
}
