package edu.cwru.eecs398.obdreader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothSocketThread extends Thread {
	
	private final BluetoothSocket socket;
	
	private final InputStream inputStream;
	private final OutputStream outputStream;
	
	public BluetoothSocketThread(BluetoothSocket btSocket) {
		this.socket = btSocket;
		InputStream tempIS = null;
		OutputStream tempOS = null;
		try {
			tempIS = socket.getInputStream();
			tempOS = socket.getOutputStream();
		} catch (IOException e) {
			Log.e("OBD", "Error getting bluetooth stream", e);
		}
		
		this.inputStream = tempIS;
		this.outputStream = tempOS;
		
	}
	
	@Override
	public void run() {
		int read = Integer.MAX_VALUE;
		try {
			while (true) {
				if (inputStream.available() > 0) {
					read = inputStream.read();
					Log.d("OBD", "Value read from socket is: " + read);
					outputStream.write(read);
					//break;
				}
			}
			
		} catch (IOException e) {
			Log.e("OBD", "Error reading from input stream", e);
		}
		
		
	}

}
